package org.alex859.sax.model;

import org.alex859.sax.annotation.XmlTagName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
@XmlTagName("person")
public class Person
{
    @XmlTagName("name")
    private String name;
    @XmlTagName("surname")
    private String surname;
    private Postcode postcode;

    private List<Address> address = new ArrayList<>();

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getSurname()
    {
        return surname;
    }

    public void setSurname(final String surname)
    {
        this.surname = surname;
    }

    public Postcode getPostcode()
    {
        return postcode;
    }

    public void setPostcode(final Postcode postcode)
    {
        this.postcode = postcode;
    }

    public List<Address> getAddress()
    {
        return address;
    }

    public void setAddress(final List<Address> address)
    {
        this.address = address;
    }

    @Override
    public String toString()
    {
        return "Person{" +
                "name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", postcode=" + postcode +
                ", address=" + address +
                '}';
    }
}
