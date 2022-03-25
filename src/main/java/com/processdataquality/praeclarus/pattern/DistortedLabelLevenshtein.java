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

package com.processdataquality.praeclarus.pattern;

import com.processdataquality.praeclarus.annotations.Pattern;
import com.processdataquality.praeclarus.annotations.Plugin;
import com.processdataquality.praeclarus.option.Options;
import org.apache.commons.text.similarity.LevenshteinDistance;
import tech.tablesaw.api.StringColumn;

/**
 * @author Michael Adams
 * @date 11/5/21
 */
@Plugin(
        name = "Levenshtein",
        author = "Michael Adams",
        version = "1.0",
        synopsis = "Calculates activity label similarity using Levenshtein Distance"
)
@Pattern(group = PatternGroup.DISTORTED_LABEL)
public class DistortedLabelLevenshtein extends AbstractImperfectLabel {

    private final LevenshteinDistance levenshtein =
            new LevenshteinDistance(getOptions().get("Threshold").asInt());

    public DistortedLabelLevenshtein() { }


    @Override
    protected void detect(StringColumn column, String s1, String s2) {
        int threshold = getOptions().get("Threshold").asInt();
        int distance = levenshtein.apply(s1, s2);
        if (distance > 0 && distance <= threshold) {
            addResult(column, s1, s2);
        }
    }

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        if (!options.containsKey("Threshold")) {
            options.addDefault("Threshold", 2);
        }
        return options;
    }

}
