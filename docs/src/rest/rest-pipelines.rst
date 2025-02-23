..
.. Copyright 2019 is-land
..
.. Licensed under the Apache License, Version 2.0 (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
..     http://www.apache.org/licenses/LICENSE-2.0
..
.. Unless required by applicable law or agreed to in writing, software
.. distributed under the License is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
..

.. _rest-pipelines:

Pipeline
========

Pipeline APIs are born of Ohara manager which needs a way to store the
relationship of components in streaming. The relationship in pipeline is
made up of multi **flows**. Each **flow** describe a **from** and multi **to**\s. For example,
you have a :ref:`topic <rest-topics>` as source and a :ref:`connector <rest-connectors>`
as consumer, so you can describe the
relationship via following flow.

  .. code-block:: json

     {
       "flows": [
         {
           "from": "topic's name",
           "to": ["connector's name"]
         }
       ]
     }

The objects grouped by pipeline should be existent. Otherwise, pipeline
will ignore them in generating object abstracts.

The objects grouped by pipeline don’t need to located on the same
cluster hierarchy. Grouping a topic, which is placed at broker_0, and a
topic, which is located at broker_1, is valid. However, the object based
on a dead cluster will get an abstract with error state.

The properties used in generating pipeline are shown below.

#. group (**string**) — pipeline’s name
#. name (**string**) — pipeline’s name
#. flows (**array(object)**) — the relationship between objects

   - flows[i].from (**object**) — the endpoint of source

     - flows[i].from.group — the group of source
     - flows[i].from.name — the name of source

   - flows[i].to (**array(object)**) — the endpoint of sinks

     - flows[i].to[j].group — the group of sink[j]
     - flows[i].to[j].name — the name of sink[j]

#. tags (**object**) — the extra description to this object



Following information are written by Ohara.

#. lastModified (**long**) — the last time to update this pipeline
#. objects (**array(object)**) — the abstract of all objects mentioned by pipeline

   - objects[i].name (**string**) — object’s name
   - objects[i].kind (**string**) — the type of this object. for instance, :ref:`topic <rest-topics>`,
     :ref:`connector <rest-connectors>`, and :ref:`streamapp <rest-stream>`
   - objects[i].className (**string**) — object’s implementation. Normally, it shows the full name of
     a java class
   - objects[i].state (**option(string)**) — the state of object. If the object can’t have state
     (eg, :ref:`topic <rest-topics>`), you won’t see this field
   - objects[i].error (**option(string)**) — the error message of this object
   - objects[i].lastModified (**long**) — the last time to update this object
   - :ref:`metrics <connector-metrics>` (**object**) — the metrics from this object.
     Not all objects in pipeline have metrics!
   - meters (**array(object)**) — the metrics in meter type
   - meters[i].value (**double**) — the number stored in meter
   - meters[i].unit (**string**) — unit for value
   - meters[i].document (**string**) — document of this meter
   - meters[i].queryTime (**long**) — the time of query metrics from remote machine
   - meters[i].startTime (**option(long)**) — the time of record generated in remote machine


create a pipeline
-----------------

*POST /v0/pipelines*

The following example creates a pipeline with a :ref:`topic <rest-topics>` and
:ref:`connector <rest-connectors>`. The :ref:`topic <rest-topics>` is created on
:ref:`broker cluster <rest-brokers>` but the :ref:`connector <rest-connectors>` isn’t. Hence,
the response from server shows that it fails to find the status of the
:ref:`connector <rest-connectors>`. That is to say, it is ok to add un-running
:ref:`connector <rest-connectors>` to pipeline.

Example Request 1
  .. code-block:: json

     {
       "name": "pipeline0",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-aaa",
           "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
         }
       ]
     }

Example Response 1
  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }

  .. note::
    Don’t worry about creating a pipeline with incomplete flows. It is ok to
    add a flow with only **from**. The following example creates a pipeline
    with only a object and leave empty in **to** field.

Example Request 1
  .. code-block:: json

     {
       "name": "pipeline1",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": []
         }
       ]
     }

Example Response 1
  .. code-block:: json

     {
       "name": "pipeline1",
       "lastModified": 1554952500972,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": []
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         }
       ],
       "tags": {}
     }


update a pipeline
-----------------

*PUT /v0/pipelines/$name*

Example Request
  .. code-block:: json

     {
       "name": "pipeline0",
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-aaa",
           "to": ["81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"]
         }
       ]
     }

  .. note::
    This API creates an new pipeline for you if the input name
    does not exist!

Example Response
  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }


list all pipelines
------------------

*GET /v0/pipelines*

Listing all pipelines is a expensive operation as it invokes a iteration
to all objects stored in pipeline. The loop will do a lot of checks and
fetch status, metrics and log from backend clusters. If you have the
name of pipeline, please use :ref:`GET <rest-pipelines-get>` to fetch details
of **single** pipeline.

the accepted query keys are listed below.
#. group
#. name
#. lastModified
#. tags

Example Response
  .. code-block:: json

     [
       {
         "name": "pipeline0",
         "lastModified": 1554950999668,
         "flows": [
           {
             "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
             "to": [
               "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
             ]
           }
         ],
         "objects": [
           {
             "group": "default",
             "name": "topic0",
             "lastModified": 1554950034608,
             "metrics": {
               "meters": []
             },
             "kind": "topic",
             "tags": {}
           },
           {
             "group": "default",
             "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
             "lastModified": 1554950058696,
             "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
             "metrics": {
               "meters": []
             },
             "kind": "connector",
             "tags": {}
           }
         ],
         "tags": {}
       }
     ]


delete a pipeline
-----------------

*DELETE /v0/pipelines/$name*

Deleting a pipeline does not delete the objects related to the pipeline.

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an an nonexistent pipeline, and the response is
     204 NoContent. However, it is illegal to remove a pipeline having any
     running objects


.. _rest-pipelines-get:

get a pipeline
--------------

*GET /v0/pipelines/$name*

Example Response
  .. code-block:: json

     {
       "name": "pipeline0",
       "lastModified": 1554950999668,
       "flows": [
         {
           "from": "be48b7d8-08a8-40a4-8f17-9c1d1fe655b6",
           "to": [
             "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd"
           ]
         }
       ],
       "objects": [
         {
           "group": "default",
           "name": "topic0",
           "lastModified": 1554950034608,
           "metrics": {
             "meters": []
           },
           "kind": "topic",
           "tags": {}
         },
         {
           "group": "default",
           "name": "81cb80a9-34a5-4e45-881a-cb87d4fbb5bd",
           "lastModified": 1554950058696,
           "error": "Failed to get status and type of connector:81cb80a9-34a5-4e45-881a-cb87d4fbb5bd. This could be a temporary issue since our worker cluster is too busy to sync status of connector. abc doesn't exist",
           "metrics": {
             "meters": []
           },
           "kind": "connector",
           "tags": {}
         }
       ],
       "tags": {}
     }


refresh a pipeline
------------------

*PUT /v0/pipelines/$name/refresh*

Requires Ohara Configurator to cleanup nonexistent objects of pipeline. Pipeline is a group of objects and it contains,
sometimes, some nonexistent objects. Those nonexistent objects won't hurt our services but it may be ugly and weird to
read. Hence, the (helper) API do a background cleanup for your pipeline. The cleanup rules are shown below.

#. the flow having nonexistent "from" is removed
#. the objects in "to" get removed

Example Response
  ::

    202 Accepted

  .. note::
    You should use :ref:`Get pipeline <rest-pipelines-get>` to fetch up-to-date status
