package org.alex859.sax;

import org.alex859.sax.model.Item;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main
{
    public static void main(String[] args) throws FileNotFoundException, XMLStreamException
	{
//        final SAXParser<Person> SAXParser = new SAXParser<>(Person.class, System.out::println);
//        SAXParser.parse(new InputSource(new FileReader("/home/alex859/DevEnv/workspace/sax/src/main/resources/test.xml")));
//        SAXParser.parse(new InputSource(new FileReader("/home/alessandro.ciccimarra/DevEnv/saxparser/src/main/resources/test.xml")));
//
//        XStream xtream = new XStream(new StaxDriver());
//        xtream.alias("person", Person.class);
//        final FileReader fileReader = new FileReader("/home/alex859/DevEnv/workspace/sax/src/main/resources/test.xml");
//        final ObjectInputStream objectInputStream = xtream.createObjectInputStream(fileReader);
//
//        final Person p = (Person) objectInputStream.readObject();

        StAXParser<Item> staxParser = new StAXParser<>(Item.class, new FileInputStream(new File("/home/alex859/DevEnv/workspace/sax/src/main/resources/test.xml")));

		while(staxParser.hasNext())
		{
			staxParser.next();
		}
    }
}
