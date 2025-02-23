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

.. _rest-nodes:

Node
====

Node is the basic unit of running service. It can be either physical
machine or vm. In section :ref:`zookeeper <rest-zookeepers>`,
:ref:`Broker <rest-brokers>` and :ref:`Worker <rest-workers>`, you will see many
requests demanding you to fill the node name to build the services.
Currently, ohara requires the node added to ohara should pre-install
following services.

#. docker (18.09+)
#. ssh server
#. k8s (only if you want to k8s to host containers)
#. official ohara images

   - `oharastream/zookeeper`_
   - `oharastream/broker`_
   - `oharastream/connect-worker`_
   - `oharastream/streamapp`_

The version (tag) depends on which ohara you used. It would be better to
use the same version to Ohara. For example, the version of Ohara
configurator you are running is 0.4, then the official images you should
download is oharastream/xxxx:0.4.

The properties used in describing a node are shown below.

#. hostname (**string**) — hostname of node.

   This hostname must be available on you DNS.
   It will cause a lot of troubles if Ohara Configurator is unable to
   connect to remote node via this hostname.

#. port (**int**) — ssh port of node
#. user (**string**) — ssh account
#. password (**string**) — ssh password
#. tags (**object**) — the extra description to this object
#. services (**array(object)**) — the services hosted by this node

   - services[i].name (**string**) — service name (configurator, zookeeper, broker, connect-worker and streamapp)
   - services[i].clusterKeys (**array(object)**) — the keys of this service

#. validationReport (**object**) — last validation result.

   This information is attached by Ohara Configurator after you request the :ref:`validation <rest-validation>`

   - validationReport.hostname (**string**) — the host which is in charge of validating node
   - validationReport.message (**string**) — the report
   - validationReport.pass (**boolean**) — true if the arguments is able to be connected
   - validationReport.lastModified (**long**) — the time to execute this validation

.. note::
   Ohara use above information to login node to manage the containers.
   Please make sure the account has permission to operate docker (and
   k8s service) without sudo.

The following information are tagged by Ohara:

#. lastModified (**long**) — the last time to update this node


store a node
------------

*POST /v0/nodes*

#. hostname (**string**) — hostname of node
#. port (**int**) — ssh port of node
#. user (**string**) — ssh account
#. password (**string**) — ssh password

Example Request
  .. code-block:: json

     {
       "hostname": "node00",
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

Example Response
  .. code-block:: json

    {
      "services": [
        {
          "name": "zookeeper",
          "clusterKeys": [
            {
              "group": "default",
              "name": "zk"
            }
          ]
        },
        {
          "name": "broker",
          "clusterKeys": []
        },
        {
          "name": "connect-worker",
          "clusterKeys": []
        },
        {
          "name": "streamapp",
          "clusterKeys": []
        }
      ],
      "hostname": "node00",
      "lastModified": 1569569857613,
      "tags": {},
      "port": 22,
      "user": "chia7712",
      "password": "jellynina0208"
    }


update a node
-------------

*PUT /v0/nodes/${name}*

#. hostname (**string**) — hostname of node
#. port (**int**) — ssh port of node
#. user (**string**) — ssh account
#. password (**string**) — ssh password

Example Request

  .. code-block:: json

     {
       "port": 22,
       "user": "abc",
       "password": "pwd"
     }

  .. note::
     An new node will be created if your input name does not exist

  .. note::
     the update request will clear the validation report attached to this node

Example Response
  .. code-block:: json

    {
      "services": [
        {
          "name": "zookeeper",
          "clusterKeys": [
            {
              "group": "default",
              "name": "zk"
            }
          ]
        },
        {
          "name": "broker",
          "clusterKeys": []
        },
        {
          "name": "connect-worker",
          "clusterKeys": []
        },
        {
          "name": "streamapp",
          "clusterKeys": []
        }
      ],
      "hostname": "node00",
      "lastModified": 1569569857613,
      "tags": {},
      "port": 22,
      "user": "chia7712",
      "password": "jellynina0208"
    }


list all nodes stored in Ohara
------------------------------

*GET /v0/nodes*

Example Response
  .. code-block:: json

    [
      {
        "services": [
          {
            "name": "zookeeper",
            "clusterKeys": [
              {
                "group": "default",
                "name": "zk"
              }
            ]
          },
          {
            "name": "broker",
            "clusterKeys": []
          },
          {
            "name": "connect-worker",
            "clusterKeys": []
          },
          {
            "name": "streamapp",
            "clusterKeys": []
          }
        ],
        "hostname": "node00",
        "lastModified": 1569569857613,
        "tags": {},
        "port": 22,
        "user": "chia7712",
        "password": "jellynina0208"
      }
    ]


delete a node
-------------

*DELETE /v0/nodes/${name}*

Example Response
  ::

     204 NoContent

  .. note::
     It is ok to delete an an nonexistent pipeline, and the response is
     204 NoContent. However, it is disallowed to remove a node which is
     running service. If you do want to delete the node from ohara, please
     stop all services from the node.

get a node
----------

*GET /v0/nodes/${name}*

Example Response
  .. code-block:: json

    {
      "services": [
        {
          "name": "zookeeper",
          "clusterKeys": [
            {
              "group": "default",
              "name": "zk"
            }
          ]
        },
        {
          "name": "broker",
          "clusterKeys": []
        },
        {
          "name": "connect-worker",
          "clusterKeys": []
        },
        {
          "name": "streamapp",
          "clusterKeys": []
        }
      ],
      "hostname": "node00",
      "lastModified": 1569569857613,
      "tags": {},
      "port": 22,
      "user": "chia7712",
      "password": "jellynina0208"
    }

.. _oharastream/zookeeper: https://cloud.docker.com/u/oharastream/repository/docker/oharastream/zookeeper
.. _oharastream/broker: https://cloud.docker.com/u/oharastream/repository/docker/oharastream/broker
.. _oharastream/connect-worker: https://cloud.docker.com/u/oharastream/repository/docker/oharastream/connect-worker
.. _oharastream/streamapp: https://cloud.docker.com/u/oharastream/repository/docker/oharastream/streamapp