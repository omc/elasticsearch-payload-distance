### elasticsearch-payload-distance

Elasticsearch plugin which scores documents by computing the distance between payload values and input values for each term.

Setting up the index for payloads:

```
curl -XPUT 'http://localhost:9200/items' -d '{
  "settings": {
    "analysis": {
      "analyzer": {
        "payloads": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": [
            "lowercase",
            "delimited_payload_filter"
          ]
        }
      }
    }
  },
  "mappings": {
    "human": {
      "properties": {
        "simterms": {
          "type": "text",
          "analyzer": "payloads",
          "term_vector": "with_positions_offsets_payloads"
        },
        "colors": {
          "type": "text",
          "analyzer": "payloads",
          "term_vector": "with_positions_offsets_payloads"
        }
      }
    }
  }
}'
```

Indexing some documents

```
curl -XPUT 'http://localhost:9200/items/outfit/1' -d '{
  "name": "flowing dress",
  "simterms": "a|100.0 b|100.0 c|100.0",
  "colors": "blue|100.0"
}'

curl -XPUT 'http://localhost:9200/items/outfit/2' -d '{
  "name": "blue dress",
  "simterms": "a|200.0 b|100.0 c|50.0",
  "colors": "green|100.0"
}'

curl -XPUT 'http://localhost:9200/items/outfit/3' -d '{
  "name": "moo dress bing",
  "simterms": "a|300.0 b|100.0 c|50.0",
  "colors": "red|100.0"
}'
```

Querying

```curl
curl -XGET 'http://localhost:9200/items/_search?pretty' -d '{
  "query": {
    "function_score": {
      "query": {
        "match": {
          "name": "dress"
        }
      },
      "script_score": {
        "script": {
          "lang": "native",
          "inline": "payload_distance_score",
          "params": {
            "fields": [
              {
                "field": "simterms",
                "term_values": {
                  "a": 100.0,
                  "b": 100.0,
                  "d": 100.0
                },
                "term_missing_factor": 0.2,
                "term_match_boost": 0.0005
              },
              {
                "field": "colors",
                "term_values": {
                  "green": 100.0
                },
                "term_missing_factor": 0.2,
                "term_match_boost": 0.0025
              }
            ]
          }
        }
      }
    }
  }
}'
```

Results

```json
{
  "took" : 3,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "hits" : {
    "total" : 3,
    "max_score" : -0.06452902,
    "hits" : [
      {
        "_index" : "items",
        "_type" : "outfit",
        "_id" : "2",
        "_score" : -0.06452902,
        "_source" : {
          "name" : "blue dress",
          "simterms" : "a|200.0 b|100.0 c|50.0",
          "colors" : "green|100"
        }
      },
      {
        "_index" : "items",
        "_type" : "outfit",
        "_id" : "1",
        "_score" : -0.10324643,
        "_source" : {
          "name" : "flowing dress",
          "simterms" : "a|100.0 b|100.0 c|100.0",
          "colors" : "blue|100"
        }
      },
      {
        "_index" : "items",
        "_type" : "outfit",
        "_id" : "3",
        "_score" : -0.12658012,
        "_source" : {
          "name" : "moo dress bing",
          "simterms" : "a|300.0 b|100.0 c|50.0",
          "colors" : "red|100"
        }
      }
    ]
  }
}
```

#### Building


```
esVersion=5.4.3 gradle clean assemble
```

#### Installation on local machine


```
cd /path/to/elasticsearch-5.4.3 && ./bin/elasticsearch-plugin install file:///path/to/elasticsearch-payload-distance-1.0.0_es5.4.3
```
