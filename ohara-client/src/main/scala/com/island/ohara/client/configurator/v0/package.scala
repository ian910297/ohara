/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.configurator

import com.island.ohara.common.setting.{ConnectorKey, ObjectKey, SettingDef, TopicKey}
import com.island.ohara.common.util.CommonUtils
import spray.json.{JsNull, JsValue, RootJsonFormat, _}

import spray.json.DefaultJsonProtocol._
package object v0 {

  /**
    * the default group to all objects.
    * the group is useful to Ohara Manager. However, in simple case, the group is a bit noisy so we offer the default group to all objects when
    * input group is ignored.
    */
  val GROUP_DEFAULT: String = "default"
  val GROUP_KEY: String = "group"

  /**
    * Noted: there are other two definition having "name"
    * 1) ConnectorDefUtils.CONNECTOR_NAME_DEFINITION
    * 2) StreamDefinitions.NAME_DEFINITION
    */
  val NAME_KEY: String = "name"

  /**
    * Noted: there are other two definition having "tags""
    * 1) ConnectorDefUtils.TAGS_DEFINITION
    * 2) StreamDefinitions.TAGS_DEFINITION
    */
  val TAGS_KEY: String = "tags"

  /**
    * Noted: there are other two definition having "nodeNames""
    * 1) StreamDefinitions.NODE_NAMES_DEFINITION
    */
  val NODE_NAMES_KEY: String = "nodeNames"
  val IMAGE_NAME_KEY: String = "imageName"
  val FORCE_KEY: String = "force"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val PAUSE_COMMAND: String = "pause"
  val RESUME_COMMAND: String = "resume"

  /**
    * There is no length limit for docker container name in current support version (18.09), but the k8s
    * docs did say that the maximum length is 253:
    * <p>https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names
    * <p>
    * We prefer to limit the sum of length with group and name since it will give us more flexibility.
    * It is worth to note that although we only restrict the "sum length", name and group fields should not
    * be empty since we forbid the empty string.
    * The length set to 100 here is enough for most case to set their group and name (each could has most 50 chars)
    */
  val LIMIT_OF_KEY_LENGTH: Int = 100

  /**
    * In this APIs we have to integrate json format between scala (spray-json) and java (jackson).
    * The JsNull generated by spray-json confuse jackson to generate many "null" object. We remove the key related to
    * JsNull in order to avoid passing null to jackson.
    */
  private[v0] def noJsNull(fields: Map[String, JsValue]): Map[String, JsValue] = fields.filter {
    _._2 match {
      case JsNull => false
      case _      => true
    }
  }

  private[v0] def noJsNull(jsValue: JsValue): Map[String, JsValue] = noJsNull(jsValue.asJsObject.fields)

  private[v0] implicit val OBJECT_KEY_FORMAT: RootJsonFormat[ObjectKey] = JsonRefiner[ObjectKey]
    .format(new RootJsonFormat[ObjectKey] {
      override def write(obj: ObjectKey): JsValue = ObjectKey.toJsonString(obj).parseJson
      override def read(json: JsValue): ObjectKey = json match {
        case JsString(s) => ObjectKey.of(GROUP_DEFAULT, s)
        case JsObject(fields) =>
          val group = noJsNull(fields)
            .get(GROUP_KEY)
            .map {
              case JsString(s) => s
              case _ =>
                throw DeserializationException(s"the type of $GROUP_KEY must be string", fieldNames = List(GROUP_KEY))
            }
            .getOrElse(GROUP_DEFAULT)
          val name = noJsNull(fields).getOrElse(NAME_KEY,
                                                throw DeserializationException(s"$NAME_KEY is required field",
                                                                               fieldNames = List(NAME_KEY))) match {
            case JsString(s) => s
            case _ =>
              throw DeserializationException(s"the type of $NAME_KEY must be string", fieldNames = List(NAME_KEY))
          }
          if (group.isEmpty) throw DeserializationException(s"$GROUP_KEY can't be empty", fieldNames = List(GROUP_KEY))
          if (name.isEmpty) throw DeserializationException(s"$NAME_KEY can't be empty", fieldNames = List(NAME_KEY))
          ObjectKey.of(group, name)
        case _ =>
          throw DeserializationException(
            "the form of key must be {\"group\": \"g\", \"name\": \"n\"}, {\"name\": \"n\"} or pure string")
      }
    })
    .nullToString(GROUP_KEY, () => GROUP_DEFAULT)
    .rejectEmptyString()
    .refine

  private[v0] implicit val TOPIC_KEY_FORMAT: RootJsonFormat[TopicKey] = new RootJsonFormat[TopicKey] {
    override def write(obj: TopicKey): JsValue = TopicKey.toJsonString(obj).parseJson
    override def read(json: JsValue): TopicKey = {
      // reuse the rules of ObjectKey
      val key = OBJECT_KEY_FORMAT.read(json)
      TopicKey.of(key.group(), key.name())
    }
  }

  private[v0] implicit val CONNECTOR_KEY_FORMAT: RootJsonFormat[ConnectorKey] = new RootJsonFormat[ConnectorKey] {
    override def write(obj: ConnectorKey): JsValue = ConnectorKey.toJsonString(obj).parseJson
    override def read(json: JsValue): ConnectorKey = {
      // reuse the rules of ObjectKey
      val key = OBJECT_KEY_FORMAT.read(json)
      ConnectorKey.of(key.group(), key.name())
    }
  }

  /**
    * exposed to configurator
    */
  private[v0] implicit val SETTING_DEFINITION_JSON_FORMAT: RootJsonFormat[SettingDef] =
    new RootJsonFormat[SettingDef] {
      override def read(json: JsValue): SettingDef = SettingDef.ofJson(json.toString())
      override def write(obj: SettingDef): JsValue = obj.toJsonString.parseJson
    }

  /**
    * use basic check rules of object key for json refiner:
    * <p> 1) name and group must satisfy the regex [a-z0-9]
    * <p> 2) name will use randomString if not defined.
    * <p> 3) group will use defaultGroup if not defined.
    * <p> 4) name length + group length <= LIMIT_OF_KEY_LENGTH
    *
    * @tparam T type of object
    * @return json refiner object
    */
  private[v0] def basicRulesOfKey[T]: JsonRefiner[T] =
    JsonRefiner[T]
    //------------------------------ "name" and "group" rules ----------------------------------//
    // we random a default name for this object
      .nullToString(NAME_KEY, () => CommonUtils.randomString(LIMIT_OF_KEY_LENGTH / 2))
      .nullToString(GROUP_KEY, () => GROUP_DEFAULT)
      .stringRestriction(Set(NAME_KEY, GROUP_KEY))
      .withNumber()
      .withLowerCase()
      .toRefiner
      //-------------------------------------- restrict rules -------------------------------------//
      // the sum of length: name + group <= LIMIT_OF_KEY_LENGTH
      .stringSumLengthLimit(Set(NAME_KEY, GROUP_KEY), LIMIT_OF_KEY_LENGTH)

  /**
    * use basic check rules of creation request for json refiner.
    * 1) reject any empty string.
    * 2) nodeName cannot use "start" and "stop" keywords.
    * 3) nodeName cannot be empty array.
    * 4) imageName will use {defaultImage} if not defined.
    * 5) tags will use empty map if not defined.
    * @param defaultImage this cluster default images
    * @tparam T type of creation
    * @return json refiner object
    */
  private[v0] def basicRulesOfCreation[T <: ClusterCreation](defaultImage: String): JsonRefiner[T] =
    basicRulesOfKey[T]
    // for each field, we should reject any empty string
      .rejectEmptyString()
      // cluster creation should use the default image of current version
      .nullToString(IMAGE_NAME_KEY, defaultImage)
      //-------------------------------------- "nodeNames" rules ---------------------------------//
      .arrayRestriction(NODE_NAMES_KEY)
      // we use the same sub-path for "node" and "actions" urls:
      // xxx/cluster/{name}/{node}
      // xxx/cluster/{name}/[start|stop]
      // the "actions" keywords must be avoided in nodeNames parameter
      .rejectKeyword(START_COMMAND)
      .rejectKeyword(STOP_COMMAND)
      // the node names can't be empty
      .rejectEmpty()
      .toRefiner
      // default is empty tags
      .nullToEmptyObject(TAGS_KEY)
      // nodeNames is the only required field in creating cluster, add the requirement for it
      .requireKey(NODE_NAMES_KEY)

  /**
    * use basic check rules of update request for json refiner.
    * 1) reject any empty string.
    * 2) nodeName cannot use "start" and "stop" keywords.
    * 3) nodeName cannot be empty array.
    * @tparam T type of update
    * @return json refiner object
    */
  private[v0] def basicRulesOfUpdating[T <: ClusterUpdating]: JsonRefiner[T] = JsonRefiner[T]
  // for each field, we should reject any empty string
    .rejectEmptyString()
    //-------------------------------------- "nodeNames" rules ---------------------------------//
    .arrayRestriction(NODE_NAMES_KEY)
    // we use the same sub-path for "node" and "actions" urls:
    // xxx/cluster/{name}/{node}
    // xxx/cluster/{name}/[start|stop]
    // the "actions" keywords must be avoided in nodeNames parameter
    .rejectKeyword(START_COMMAND)
    .rejectKeyword(STOP_COMMAND)
    // the node names can't be empty
    .rejectEmpty()
    .toRefiner

  /**
    * there are many objects containing "settings", and it is filterable so we separate the related code for reusing.
    *
    * @param settings settings
    * @param key key
    * @param value string of json representation. Noted the string of json string is pure "string" (no quote)
    * @return true if the key-value is matched. Otherwise, false
    */
  def matchSetting(settings: Map[String, JsValue], key: String, value: String): Boolean = settings.get(key).exists {
    // it is impossible to have JsNull since our json format does a great job :)
    case JsString(s)  => s == value
    case JsNumber(i)  => i == BigDecimal(value)
    case JsBoolean(b) => b == value.toBoolean
    case js: JsArray =>
      value.parseJson match {
        case other: JsArray => other == js
        case _              => false
      }
    case js: JsObject =>
      value.parseJson match {
        case other: JsObject => other == js
        case _               => false
      }
    case _ => false
  }

  /**
    * Compare the optional argument.
    *
    * Noted that it returns true if the optionString is empty and the query value is "none"
    * Noted that the comparison is case-insensitive
    * @param optionString option argument
    * @param value query value
    * @return true if matched. Otherwise, false
    */
  def matchOptionString(optionString: Option[String], value: String): Boolean =
    optionString.exists(_.toLowerCase == value.toLowerCase) || (optionString.isEmpty && value.toLowerCase == "none")

  def matchArray(array: Set[String], value: String): Boolean = value.parseJson match {
    case JsArray(es) =>
      if (es.forall(_.isInstanceOf[JsString])) es.map(_.convertTo[String]).toSet == array
      else false
    case _ => false
  }
}
