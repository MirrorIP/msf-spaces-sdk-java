/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.imc.mirror.sdk.java.packet;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.XMLOutputter;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.xmlpull.v1.XmlPullParser;

/**
 * Replaces the original item parser which doesn't transfer namespace information. 
 */
public class ItemProvider implements PacketExtensionProvider {
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		String id = parser.getAttributeValue(null, "id");
		String node = parser.getAttributeValue(null, "node");
		String elementName = parser.getName();
		
		PayloadItem<?> item = null;
		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				item = parsePayload(id, node, parser);
				break;
			case XmlPullParser.END_TAG:
				if (elementName.equals(parser.getName())) {
					done = true;
				}
				break;
			}
		}
		return item;
	}

	private PayloadItem<?> parsePayload(String id, String node, XmlPullParser parser) throws Exception {
		String elementName = parser.getName();
		String namespace = parser.getNamespace();

		if (ProviderManager.getInstance().getExtensionProvider(elementName, namespace) != null) {
			return new PayloadItem<PacketExtension>(id, node, PacketParserUtils.parsePacketExtension(elementName, namespace, parser));
		} else {
			Element element = new Element(elementName, namespace);
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributePrefix(i) != null) {
					Namespace attributeNamespace = Namespace.getNamespace(parser.getAttributePrefix(i), parser.getAttributeNamespace(i));
					element.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i), attributeNamespace);
				} else {
					element.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
				}
			}
			boolean done = false;
			while (!done) {
				int event = parser.next();
				switch (event) {
				case XmlPullParser.START_TAG:
					element.addContent(parseArbitraryElement(parser));
					break;
				case XmlPullParser.TEXT:
					element.setText(parser.getText());
					break;
				case XmlPullParser.END_TAG:
					if (parser.getName().equals(elementName)) {
						done = true;
					}
				}
			}
			String elementString = new XMLOutputter().outputString(element);
			return new PayloadItem<SimplePayload>(id, node, new SimplePayload(elementName, namespace, elementString));
		}
	}

	private Element parseArbitraryElement(XmlPullParser parser) throws Exception {
		String elementName = parser.getName();
		String namespace = parser.getNamespace();
		Element element = new Element(elementName, namespace);
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			element.setAttribute(parser.getAttributeName(i),
					parser.getAttributeValue(i));
		}

		boolean done = false;
		while (!done) {
			int event = parser.next();
			switch (event) {
			case XmlPullParser.START_TAG:
				element.addContent(parseArbitraryElement(parser));
				break;
			case XmlPullParser.TEXT:
				element.setText(parser.getText());
				break;
			case XmlPullParser.END_TAG:
				if (parser.getName().equals(elementName)) {
					done = true;
				}
			}
		}
		return element;
	}

}
