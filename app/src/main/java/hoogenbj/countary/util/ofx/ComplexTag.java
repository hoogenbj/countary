/*
 * Copyright (c) 2023. Johan Hoogenboezem
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package hoogenbj.countary.util.ofx;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class ComplexTag extends Tag {
    private List<Tag> children = new ArrayList<>();
    private ComplexTag parent;

    public ComplexTag(ComplexTag parent, String name) {
        super(name);
        this.parent = parent;
    }

    public List<Tag> getChildren() {
        return children;
    }

    @Override
    public Tag processLine(String line) {
        Matcher simpleTag = Tags.SIMPLE_TAG_REGEX.matcher(line);
        if (simpleTag.matches()) {
            children.add(new SimpleTag(this, simpleTag.group(1), simpleTag.group(2)));
            return this;
        }
        Matcher complexTag = Tags.COMPLEX_TAG_REGEX.matcher(line);
        if (complexTag.matches()) {
            ComplexTag child = new ComplexTag(this, complexTag.group(1));
            children.add(child);
            return child;
        }
        Matcher endTag = Tags.END_TAG_REGEX.matcher(line);
        if (endTag.matches()) {
            return parent;
        }
        throw new RuntimeException(String.format("Unexpected end of OFX processing at line: [%s]", line));
    }
}
