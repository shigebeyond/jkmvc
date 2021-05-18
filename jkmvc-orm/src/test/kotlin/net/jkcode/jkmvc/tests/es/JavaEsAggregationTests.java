package net.jkcode.jkmvc.tests.es;

import org.elasticsearch.search.aggregations.*;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class JavaEsAggregationTests {

    public static void main(String[] args) {
        // 构建聚合查询
        // 每个队伍 -- select count(position), sum(salary), sum(games.score), team from player_index group by team;
        TermsAggregationBuilder teamAgg = AggregationBuilders.terms("team").order(Terms.Order.aggregation("games>sum_games_score", false));
        CardinalityAggregationBuilder cardinalityPositionAgg = AggregationBuilders.cardinality("cardinality_position ").field("position");
        SumAggregationBuilder sumSalaryAgg = AggregationBuilders.sum("sum_salary").field("salary");
        teamAgg.subAggregation(cardinalityPositionAgg);
        teamAgg.subAggregation(sumSalaryAgg);
        // 子文档
        NestedAggregationBuilder nestedGamesAgg1 = AggregationBuilders.nested("games", "games");
        SumAggregationBuilder sumScoreAgg1 = AggregationBuilders.sum("sum_games_score").field("games.score");
        nestedGamesAgg1.subAggregation(sumScoreAgg1);
        teamAgg.subAggregation(nestedGamesAgg1);

        // 每个队伍+职位 -- select avg(age), team, position from player_index group by team, position;
        TermsAggregationBuilder subPositionAgg = AggregationBuilders.terms("position");
        AvgAggregationBuilder avgAgeAgg = AggregationBuilders.avg("avg_age").field("age");
        subPositionAgg.subAggregation(avgAgeAgg);
        teamAgg.subAggregation(subPositionAgg);

        // 每个职位 -- select avg(salary), sum(games.score), position from player_index group by position; -- sum(games.score)不能执行
        TermsAggregationBuilder positionAgg = AggregationBuilders.terms("position").order(Terms.Order.aggregation("games>sum_games_score", false));
        AvgAggregationBuilder avgSalaryAgg = AggregationBuilders.avg("avg_salary").field("salary");
        positionAgg.subAggregation(avgSalaryAgg);
        // 子文档
        NestedAggregationBuilder nestedGamesAgg2 = AggregationBuilders.nested("games", "games");
        SumAggregationBuilder sumScoreAgg2 = AggregationBuilders.sum("sum_games_score").field("games.score");
        nestedGamesAgg2.subAggregation(sumScoreAgg2);
        positionAgg.subAggregation(nestedGamesAgg2);

        // 每场比赛 -- select sum(games.score) from  player_index group by games.id
        NestedAggregationBuilder nestedGamesAgg3 = AggregationBuilders.nested("games", "games");
        TermsAggregationBuilder subGameIdAgg = AggregationBuilders.terms("games.id").order(Terms.Order.aggregation("sum_games_score", false));
        SumAggregationBuilder sumScoreAgg3 = AggregationBuilders.sum("sum_games_score").field("games.score");
        subGameIdAgg.subAggregation(sumScoreAgg3);
        nestedGamesAgg3.subAggregation(subGameIdAgg);

        SearchSourceBuilder nativebuilder = new SearchSourceBuilder();
        nativebuilder.aggregation(teamAgg);
        nativebuilder.aggregation(positionAgg);
        nativebuilder.aggregation(nestedGamesAgg3);

        System.out.print(nativebuilder);
    }
}
