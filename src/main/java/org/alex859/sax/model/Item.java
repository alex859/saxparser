package org.alex859.sax.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ITEM")
public class Item
{
	@XmlElement(name = "TRANSACTION_TYPE")
	private String transactionType;
	@XmlElement(name = "SKU")
	private String sku;
	@XmlElement(name = "QUANTITY")
	private String quantity;
	@XmlElement(name = "TIMESTAMP")
	private String timestamp;

	public String getTransactionType()
	{
		return transactionType;
	}

	public void setTransactionType(final String transactionType)
	{
		this.transactionType = transactionType;
	}

	public String getSku()
	{
		return sku;
	}

	public void setSku(final String sku)
	{
		this.sku = sku;
	}

	public String getQuantity()
	{
		return quantity;
	}

	public void setQuantity(final String quantity)
	{
		this.quantity = quantity;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(final String timestamp)
	{
		this.timestamp = timestamp;
	}

	@Override
	public String toString()
	{
		return "Item{" +
				"transactionType='" + transactionType + '\'' +
				", sku='" + sku + '\'' +
				", quantity='" + quantity + '\'' +
				", timestamp='" + timestamp + '\'' +
				'}';
	}
}