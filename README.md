# AI

Things related to artificial intelligence operating on top of other Nasdanika capabilities.
Specifically, operating on top of resource sets - collections of interconnected models. 
Models and resource sets abstract AI components from low level implementation details.

"Narrator" processors which describe model elements and their relationships in multiple ways.
For example, in the [sample family](https://nasdanika-demos.github.io/family-semantic-mapping/)
the model with derived relationships and capability-based reasoning can be used to explain that:

* Paul is a parent of Lea
* Paul is a father of Lea
* Lea is a child of Paul
* Lea is a daughter of Paul
* Elias is a sibling of Lea
* Elias is a brother of Lea
* ...

Also, using EObject -> EClass relationship it may be explained that Lea is a woman and then a definition
of a woman from the metamodel can be given.

This text can be added to Lea's description. Then the resulting text can be chunked, embedings can be generated and
added to a vector store.
This can be used for semantic search and RAG which takes not only semantic distance, but also graph distance into
account.

For example, for a question/chat in Dave's context semantic matches from Elias would have
higher weight/smaller distance than matches from Paul.

Key components:

* Embeddings - from OpenAI and Ollams
* Vector store - https://github.com/jelmerk/hnswlib. Observations from a local computer:
    * ~ 10 minutes to load 200K vectors from https://dl.fbaipublicfiles.com/fasttext/vectors-crawl/cc.en.300.vec.gz previously downloaded
    * ~ 2.5 Gb index file
    * ~ 90 sec to load index from the file
* Chat completions
* CLI
    * Vector store mixin
    * Embeddings generator for search-documents.json
    * Vector store generator from search-documents.json with embeddings or not. Brand-new or add/update existing
    * Semantic search HTTP Server routes
    * Chat Vuejs component - adapt https://medium.com/@alen.ajam/building-a-simple-chat-app-with-vue-js-462c4a53c6ad
    * Chatting with a site - serve both a static site and semantic search command, OpenAI and Ollama. Docker on a GCP VM.
    * Chatting with a model
    




