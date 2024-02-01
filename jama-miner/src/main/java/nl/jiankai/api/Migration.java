package nl.jiankai.api;

public record Migration(ApiMapping mapping, Migration next) { }
