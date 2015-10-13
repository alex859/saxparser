package org.alex859.sax;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;

import static javax.xml.stream.XMLStreamConstants.*;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
public class StAXParser
{
    private final XMLStreamReader reader;

    public StAXParser(final FileInputStream fileInputStream) throws XMLStreamException
    {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        reader = factory.createXMLStreamReader(fileInputStream);
    }
    public Object getNext() throws XMLStreamException
    {
        while (reader.hasNext())
        {
            int event = reader.next();

            switch (event)
            {
                case START_ELEMENT:
                    System.out.println(reader.getLocalName() + ": " + reader.getElementText());
                    break;
            }
        }
        return null;
    }
}
