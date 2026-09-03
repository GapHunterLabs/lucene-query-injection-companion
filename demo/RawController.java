import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class RawController {
    @GetMapping("/search")
    Object run(String userQuery) {
        return QueryBuilders.queryStringQuery(userQuery);
    }
}
