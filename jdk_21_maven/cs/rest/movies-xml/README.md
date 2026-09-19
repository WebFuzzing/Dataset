# movies-xml

<img width="1724" height="848" alt="image" src="https://github.com/user-attachments/assets/3190d5b4-e3e4-4dee-be2a-22c34e5d48d2" />

A movie catalogue REST API whose request and response bodies are **exclusively
`application/xml`**. There is no JSON path anywhere in the API: a write sent as
`application/json` is answered with `415`, and errors come back as XML too.

The project is a case study for research on REST API fuzzing. What is being measured is
whether a fuzzer can build XML request bodies that honour the `xml` metadata of an
OpenAPI schema — attributes instead of elements, a root element whose name differs from
the class name, and a wrapped list. The database therefore starts **empty**: nothing is
preloaded, so every row either arrives through a well formed XML request or through a
direct SQL insert.

The one accepted shape of a movie document is:

```xml
<movie id="7" year="1999">
    <title>The Matrix</title>
    <genre>SCIFI</genre>
    <rating>8.7</rating>
    <director nationality="US">
        <name>Lana Wachowski</name>
    </director>
    <cast>
        <actor billing="1"><name>Keanu Reeves</name></actor>
        <actor billing="2"><name>Laurence Fishburne</name></actor>
    </cast>
</movie>
```

`id` and `year` are attributes, `nationality` is an attribute of the nested `<director>`,
and the cast is a `<cast>` wrapper around `<actor>` elements. Nothing else is accepted:
spelling `id` or `year` as elements, or hanging `<actor>` straight off `<movie>`, is a
`400`. On creation a supplied `id` is ignored and the server assigns its own.

## Build and run

Java 21 and Maven are required.

```bash
mvn clean package
java -jar target/movies-xml-sut.jar
```

The app listens on port 8080 and stores everything in an in-memory H2 database that is
created at startup and dropped at shutdown. The H2 console is disabled.

## Endpoints

Seven, all under `/movies`:

| Method | Path | Responses |
|---|---|---|
| `POST` | `/movies` | 201 + `Location`, 400, 409, 415 |
| `GET` | `/movies` | 200 (`<movies>`), 400 |
| `GET` | `/movies/{id}` | 200, 404 |
| `PUT` | `/movies/{id}` | 200, 400, 404, 409, 415 |
| `PATCH` | `/movies/{id}` | 200, 400, 404, 409, 415 |
| `DELETE` | `/movies/{id}` | 204, 404 |
| `GET` | `/movies/stats` | 200 (`<stats>`) |

Two more answers are not tied to any one endpoint, because they come from the exception
handler rather than from a controller: `405` for a method the path does not support and
`406` for a client that asks for anything other than `application/xml`. Every endpoint can
produce both, and the schema says so on every operation.

The handler also turns anything unexpected into a `500`, and that one is deliberately left
out of the schema. This is a fuzzing target: a `500` is a fault worth reporting, not an
answer a tool should be told to expect. No request is known to produce one.

Anything in the `{id}` position that is not a stored movie is a `404`, including an id that
is not a number at all (`/movies/abc`), one too large for a 64 bit integer, and a blank one
(`/movies/%20`). The `400` on `GET /movies` is for the query parameters, which are part of
the request rather than of the path. The one gap is a URI Tomcat refuses to parse — a null
byte or an encoded traversal, as in `/movies/%00` — which is rejected with an HTML `400`
before the application runs at all.

`GET /movies` takes `genre`, `titleContains`, `minYear`, `maxYear`, `minRating` and
`sort` (`title`, `year` or `rating`); they are optional and combinable.

`PATCH` applies only the fields the document actually carries, and it tells an absent
`<cast>` (the cast is left alone) apart from an empty `<cast></cast>` (the cast is
cleared).

## The OpenAPI schema

The schema is served as JSON at `/v3/api-docs` — it is the schema, not part of the API —
and Swagger UI renders it at `/swagger-ui.html`.

A snapshot of it is committed as [`openapi.json`](openapi.json), so the exact contract
can be read, diffed and reviewed without starting the application. It is what a client
has to go on, including the `xml` metadata: which properties are attributes, which
element names differ from the property names, and that `cast` is a wrapped list.

**`openapi.json` is generated. Never edit it by hand.** Regenerate it whenever the
endpoints, the DTOs or their annotations change: start the application, then run this
from the repository root and commit the result along with the change that caused it.

```powershell
python -c "import json,urllib.request; d=json.load(urllib.request.urlopen('http://localhost:8080/v3/api-docs')); open('openapi.json','w',newline='\n',encoding='utf-8').write(json.dumps(d,indent=2,ensure_ascii=False)+'\n')"
```

It writes two-space indentation and LF endings, so a regeneration that changed nothing
produces an empty diff. Piping `curl` into `python` on Windows PowerShell does not work
here — the pipeline prepends a BOM that `json.load` rejects — which is why the command
downloads the document itself.

## Creating a movie

```bash
curl -i -X POST http://localhost:8080/movies \
  -H 'Content-Type: application/xml' \
  -d '<movie year="1999"><title>The Matrix</title><genre>SCIFI</genre><rating>8.7</rating><director nationality="US"><name>Lana Wachowski</name></director><cast><actor billing="1"><name>Keanu Reeves</name></actor></cast></movie>'
```

```
HTTP/1.1 201
Location: http://localhost:8080/movies/1
Content-Type: application/xml

<movie id="1" year="1999">...</movie>
```

Errors use a single document shape:

```xml
<error status="404">
    <message>movie 999 not found</message>
</error>
```

## Rules worth knowing

- No two movies may share a title and a year (title compared ignoring case and
  surrounding whitespace) — `409`.
- `billing` values must be unique inside a cast — `400`.
- A movie rated above 9.0 must list at least one actor — `400`.
- `nationality`, when given, is exactly two upper case letters — `400`.
- Statistics skip genres with no movies, and average only the movies that carry a
  rating.
