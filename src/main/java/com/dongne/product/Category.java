package com.dongne.product;

public enum Category {
	DIGITAL("디지털기기"), FURNITURE("가구/인테리어"), CLOTHING("의류"), SPORTS("스포츠/레저"),
	LIVING("생활/주방"), BEAUTY("뷰티/미용"), PET("반려동물용품"), BOOK("도서"), ETC("기타");

	private final String label;

	Category(String label) { this.label = label; }

	public String getLabel() { return label; }
}
