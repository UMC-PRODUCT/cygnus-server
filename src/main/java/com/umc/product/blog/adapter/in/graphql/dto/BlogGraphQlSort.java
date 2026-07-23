package com.umc.product.blog.adapter.in.graphql.dto;

public final class BlogGraphQlSort {

    private BlogGraphQlSort() {
    }

    public enum Content {
        PUBLISHED_AT_DESC("publishedAt,desc"),
        PUBLISHED_AT_ASC("publishedAt,asc");

        private final String value;

        Content(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Series {
        CREATED_AT_DESC("createdAt,desc"),
        CREATED_AT_ASC("createdAt,asc");

        private final String value;

        Series(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Comment {
        CREATED_AT_DESC("createdAt,desc"),
        CREATED_AT_ASC("createdAt,asc");

        private final String value;

        Comment(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
