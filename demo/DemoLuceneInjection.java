import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class DirectQueryController {
    @GetMapping("/search")
    Object run(String userQuery) {
        Object query = QueryBuilders.queryStringQuery(userQuery);
        return query;
    }
}

class ConcatValidController {
    @GetMapping("/search/by-title")
    Object run(String userQuery) {
        Object query = QueryBuilders.queryStringQuery("title:" + userQuery);
        return query;
    }
}

class SimpleQueryStringController {
    @PostMapping("/search/simple")
    Object run(String rawInput) {
        Object query = QueryBuilders.simpleQueryStringQuery(rawInput);
        return query;
    }
}

class ConcatInvalidController {
    @GetMapping("/search/grouped")
    Object run(String userQuery) {
        Object query = QueryBuilders.queryStringQuery("title:\"unterminated" + userQuery);
        return query;
    }
}

class NumericParamController {
    @GetMapping("/search/range")
    Object run(int rangeStart) {
        Object query = QueryBuilders.queryStringQuery("priority:[" + rangeStart + " TO 100]");
        return query;
    }
}

class NotEndpointController {
    Object run(String userQuery) {
        Object query = QueryBuilders.queryStringQuery(userQuery);
        return query;
    }
}
