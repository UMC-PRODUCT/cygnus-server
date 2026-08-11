package com.umc.product.global.graphql.relay;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

@Controller
public class NodeGraphQlController {

    private final Map<String, NodeFetcher> fetchersByTypeName;

    public NodeGraphQlController(List<NodeFetcher> nodeFetchers) {
        this.fetchersByTypeName = nodeFetchers.stream()
            .collect(Collectors.toUnmodifiableMap(NodeFetcher::typeName, Function.identity()));
    }

    @QueryMapping
    @Nullable public RelayNode node(@Argument String id) {
        GlobalId globalId = GlobalId.decode(id);
        NodeFetcher fetcher = fetchersByTypeName.get(globalId.typeName());
        return fetcher == null ? null : fetcher.fetchOrNull(globalId.rawIdAsLong());
    }

    @QueryMapping
    public List<RelayNode> nodes(@Argument List<String> ids) {
        return ids.stream()
            .map(this::node)
            .toList();
    }
}
