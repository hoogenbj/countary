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

package hoogenbj.countary.util;

import hoogenbj.countary.util.ofx2.Header;
import hoogenbj.countary.util.ofx2.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;

public class OFX2StatementParser implements StatementParser {
    @Override
    public ParsedStatement parse(URI uri) throws IOException, StatementParseException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(uri.toString());
            NodeList nodes = document.getChildNodes();
            Header header = null;
            Node body = null;
            Node unknown = null;
            for (int i = 0; i< nodes.getLength(); i++) {
                if (nodes.item(i).getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)
                    header = Parser.parseHeader(nodes.item(i), uri.toString());
                else if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE)
                    body = nodes.item(i);
                else
                    unknown = nodes.item(i);
            }
            if (Parser.validate(header, body, uri.toString()) && unknown == null)
                return Parser.parseBody(body, uri.toString());
            else if (unknown != null)
                throw new StatementParseException("Unexpected content "+unknown, uri.toString());
            else
                throw new StatementParseException("Statement does not appear to be valid. Wrong version perhaps", uri.toString());
        } catch (ParserConfigurationException | SAXException e) {
            throw new StatementParseException(uri.toString(), e);
        }
    }
}
