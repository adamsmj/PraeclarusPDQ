/*
 * Copyright (c) 2021 Queensland University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.processdataquality.praeclarus.ui.canvas;

import com.processdataquality.praeclarus.logging.EventLogger;
import com.processdataquality.praeclarus.graph.Graph;
import com.processdataquality.praeclarus.node.Node;
import com.processdataquality.praeclarus.node.NodeLoader;
import com.processdataquality.praeclarus.repo.graph.GraphStore;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2/6/21
 */
public class WorkflowLoader {

    private final Workflow _workflow;      // frontend

    public WorkflowLoader(Workflow workflow) {
        _workflow = workflow;
    }


    public void load(String jsonStr) throws JSONException, IOException {
        EventLogger.ignoreEvents();                  // suppress events while loading
        JSONObject json = new JSONObject(jsonStr);
        Graph graph = loadGraph(json, jsonStr);
        _workflow.clear(graph);
        _workflow.setLoading(true);
        Map<String, Vertex> vertices = loadVertices(json.getJSONArray("vertices"));
        loadConnectors(json.getJSONArray("connectors"), vertices);
        _workflow.setLoading(false);
        EventLogger.captureEvents();
        selectHeadVertex(vertices, graph);
    }


    private Graph loadGraph(JSONObject json, String content) throws JSONException {

        // check if this one is persisted
        String id = json.getString("id");
        Optional<Graph> optional = GraphStore.get(id);
        if (optional.isPresent()) {
            Graph graph = optional.get();
            graph.refreshOptions();
            return graph;
        }

        // unknown to this deployment
        Graph.Builder builder = new Graph.Builder(json.getString("creator"))
                .id(id)
                .name(json.getString("name"))
                .owner(json.getString("owner"))
                .creationTime(strToDateTime(json.getString("creationTime")))
                .userContent(content);

        String description = json.optString("description");
        if (StringUtils.isNotEmpty(description)) {
            builder.description(description);
        }

        String lastSaved = json.optString("lastSavedTime");
        if (StringUtils.isNotEmpty(lastSaved)) {
            builder.lastSavedTime(strToDateTime(lastSaved));
        }
        
        return builder.build();
    }


    private Map<String, Vertex> loadVertices(JSONArray array)
            throws JSONException, IOException {
        Map<String, Vertex> vertexMap = new HashMap<>();
        NodeLoader nodeLoader = new NodeLoader();
        for (int i=0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            double x = json.getDouble("x");
            double y = json.getDouble("y");
            Node node = nodeLoader.fromJson(json.getJSONObject("node"));
            if (node != null) {
                Vertex vertex = new Vertex(x, y, node);
                _workflow.addVertex(vertex);
                if (node.hasCompleted()) {
                    vertex.setRunState(VertexStateIndicator.State.COMPLETED);
                }
                vertexMap.put(vertex.getID(), vertex);
            }
        }
        return vertexMap;
    }


    private void loadConnectors(JSONArray array, Map<String, Vertex> vertices)
            throws JSONException {
        for (int i=0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            String sourceID = json.getString("source");
            String targetID = json.getString("target");
            Vertex source = vertices.get(sourceID);
            Vertex target = vertices.get(targetID);
            if (! (source == null || target == null)) {
                Port sourcePort = source.getOutputPort();
                Port targetPort = target.getInputPort();
                _workflow.addConnector(new Connector(sourcePort, targetPort));
            }
        }
    }


    private void selectHeadVertex(Map<String, Vertex> vertices, Graph graph) {
        if (! vertices.isEmpty()) {
            Set<Node> heads = graph.getHeads();
            if (! heads.isEmpty()) {
                _workflow.setSelectedNode(heads.iterator().next());       // set any head
            }
        }
    }


    private LocalDateTime strToDateTime(String s) {
        return LocalDateTime.parse(s, EventLogger.dtFormatter);
    }

}
