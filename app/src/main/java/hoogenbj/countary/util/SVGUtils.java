/*
 * Copyright (c) 2022. Johan Hoogenboezem
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

package hoogenbj.countary.util;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class SVGUtils {
    public static SVGPath makeDeleteIcon() {
        SVGPath icon = new SVGPath();
        icon.setFill(Color.BLACK);
        String trashcanContent = "M9,3V4H4V6H5V19A2,2 0 0,0 7,21H17A2,2 0 0,0 19,19V6H20V4H15V3H9M9,8H11V17H9V8M13,8H15V17H13V8Z";
        icon.setContent(trashcanContent);
        return icon;
    }

    public static SVGPath makeRefreshIcon() {
        SVGPath icon = new SVGPath();
        icon.setFill(Color.BLACK);
        String content = "M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z";
        icon.setContent(content);
        return icon;
    }

    public static SVGPath makeCopyIcon() {
        SVGPath icon = new SVGPath();
        icon.setFill(Color.BLACK);
        String content = "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z";
        icon.setContent(content);
        return icon;
    }
}
