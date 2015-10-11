//package org.alex859.sax;
//
//import org.alex859.sax.model.Address;
//import org.alex859.sax.model.Person;
//import org.apache.commons.beanutils.PropertyUtils;
//import org.xml.sax.Attributes;
//import org.xml.sax.ContentHandler;
//import org.xml.sax.SAXException;
//import org.xml.sax.XMLReader;
//import org.xml.sax.helpers.DefaultHandler;
//
//import java.io.CharArrayWriter;
//import java.lang.reflect.InvocationTargetException;
//
///**
// * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
// */
//public class PersonHandler extends DefaultHandler
//{
//    private final CharArrayWriter contents = new CharArrayWriter();
//    private final XMLReader xmlReader;
//    private final AddressHandler addressHandler = new AddressHandler();
//
//    private Person person;
//
//    public PersonHandler(final XMLReader xmlReader)
//    {
//        this.xmlReader = xmlReader;
//    }
//
//    @Override
//    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
//    {
//        contents.reset();
//
//        switch (qName)
//        {
//            case "person":
//                person = new Person();
//                break;
//            case "address":
//                final Address address = new Address();
//                person.setAddress(address);
//                addressHandler.collectAddress(address, this, xmlReader);
//                break;
//        }
//    }
//
//    @Override
//    public void endElement(final String uri, final String localName, final String qName) throws SAXException
//    {
//        if("person".equals(qName))
//        {
//
//        }
//        else
//        {
//            try
//            {
//                PropertyUtils.setProperty(person, qName, contents.toString());
//            }
//            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
//            {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public void characters(final char[] ch, final int start, final int length) throws SAXException
//    {
//        contents.write(ch, start, length);
//    }
//
//    static class AddressHandler extends DefaultHandler
//    {
//        private final CharArrayWriter contents = new CharArrayWriter();
//        private ContentHandler parentHandler;
//        private XMLReader xmlReader;
//        private Address currentAddress;
//
//        public void collectAddress(final Address address, final ContentHandler parentHandler, final XMLReader xmlReader)
//        {
//            this.parentHandler = parentHandler;
//            this.xmlReader = xmlReader;
//            this.xmlReader.setContentHandler(this);
//
//            this.currentAddress = address;
//        }
//
//        @Override
//        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException
//        {
//            contents.reset();
//        }
//
//        @Override
//        public void endElement(final String uri, final String localName, final String qName) throws SAXException
//        {
//            if ("address".equals(qName))
//            {
//                xmlReader.setContentHandler(parentHandler);
//            }
//            else
//            {
//                try
//                {
//                    PropertyUtils.setProperty(currentAddress, qName, contents.toString());
//                }
//                catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        @Override
//        public void characters(final char[] ch, final int start, final int length) throws SAXException
//        {
//            contents.write(ch, start, length);
//        }
//    }
//
//    public Person getPerson()
//    {
//        return person;
//    }
//}
