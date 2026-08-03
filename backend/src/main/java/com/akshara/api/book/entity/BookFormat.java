package com.akshara.api.book.entity;

public enum BookFormat {
    PAPERBACK,
    HARDCOVER,
    PDF,
    EPUB;

    public boolean isPhysical() {
        return this == PAPERBACK || this == HARDCOVER;
    }

    public boolean isDigital() {
        return !isPhysical();
    }
}
