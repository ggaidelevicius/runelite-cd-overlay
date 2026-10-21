package com.cooldownoverlays;

public enum BorderStyle
{
	ITEM_OUTLINE("Item outline"),
	INVENTORY_SLOT("Inventory slot");

	private final String label;

	BorderStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
