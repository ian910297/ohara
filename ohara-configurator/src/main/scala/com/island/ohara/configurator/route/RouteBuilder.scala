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

package com.island.ohara.configurator.route

import java.util.Objects

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{
  as,
  complete,
  delete,
  entity,
  get,
  parameter,
  parameterMap,
  path,
  pathEnd,
  pathPrefix,
  post,
  put,
  _
}
import akka.http.scaladsl.server.Route
import com.island.ohara.client.configurator.v0.{BasicCreation, OharaJsonFormat}
import com.island.ohara.client.configurator.{Data, QueryRequest}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.route.hook._
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, RootJsonFormat}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Ohara offers ~100 APIs to construct the unparalleled streaming platform. However, the routes generated by many APIs
  * almost kill us in maintaining and updating. In order to save our developers from understanding the complicated routes
  * , this class extract the basic rules to be base for most routes.
  * @tparam Creation creation request
  * @tparam Updating updating request
  * @tparam Res response
  */
trait RouteBuilder[Creation <: BasicCreation, Updating, Res <: Data]
    extends com.island.ohara.common.pattern.Builder[server.Route] {
  private[this] var root: String = _
  private[this] var hookOfCreation: HookOfCreation[Creation, Res] = _
  private[this] var hookOfUpdating: HookOfUpdating[Updating, Res] = _
  private[this] var hookOfGet: HookOfGet[Res] = (res: Res) => Future.successful(res)
  private[this] var hookOfList: HookOfList[Res] = (res: Seq[Res]) => Future.successful(res)
  private[this] var hookBeforeDelete: HookBeforeDelete = (_: ObjectKey) => Future.successful(Unit)
  private[this] val hookOfPutActions: mutable.Map[String, HookOfAction[Res]] = mutable.Map[String, HookOfAction[Res]]()
  private[this] var hookOfFinalPutAction: Option[HookOfAction[Res]] = None
  private[this] val hookOfDeleteActions: mutable.Map[String, HookOfAction[ObjectKey]] =
    mutable.Map[String, HookOfAction[ObjectKey]]()
  private[this] var hookOfFinalDeleteAction: Option[HookOfAction[ObjectKey]] = None

  def root(root: String): RouteBuilder[Creation, Updating, Res] = {
    this.root = CommonUtils.requireNonEmpty(root)
    this
  }

  def hookOfCreation(hookOfCreation: HookOfCreation[Creation, Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfCreation = Objects.requireNonNull(hookOfCreation)
    this
  }

  def hookOfUpdating(hookOfUpdating: HookOfUpdating[Updating, Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfUpdating = Objects.requireNonNull(hookOfUpdating)
    this
  }

  @Optional("add custom hook to process the returned result")
  def hookOfGet(hookOfGet: HookOfGet[Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfGet = Objects.requireNonNull(hookOfGet)
    this
  }

  @Optional("add custom hook to process the returned result")
  def hookOfList(hookOfList: HookOfList[Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfList = Objects.requireNonNull(hookOfList)
    this
  }

  @Optional("add custom hook to process the result before deleting it")
  def hookBeforeDelete(hookBeforeDelete: HookBeforeDelete): RouteBuilder[Creation, Updating, Res] = {
    this.hookBeforeDelete = Objects.requireNonNull(hookBeforeDelete)
    this
  }

  @Optional("add custom hook to response specific PUT action")
  def hookOfPutAction(action: String, hookOfAction: HookOfAction[Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfPutActions += (CommonUtils.requireNonEmpty(action) -> Objects.requireNonNull(hookOfAction))
    this
  }

  @Optional("add custom hook to response remaining PUT action")
  def hookOfFinalPutAction(hookOfFinalPutAction: HookOfAction[Res]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfFinalPutAction = Some(hookOfFinalPutAction)
    this
  }

  @Optional("add custom hook to response specific DELETE action")
  def hookOfDeleteAction(action: String,
                         hookOfAction: HookOfAction[ObjectKey]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfDeleteActions += (CommonUtils.requireNonEmpty(action) -> Objects.requireNonNull(hookOfAction))
    this
  }

  @Optional("add custom hook to response remaining DELETE action")
  def hookOfFinalDeleteAction(
    hookOfFinalDeleteAction: HookOfAction[ObjectKey]): RouteBuilder[Creation, Updating, Res] = {
    this.hookOfFinalDeleteAction = Some(hookOfFinalDeleteAction)
    this
  }

  override def build(): Route = doBuild(
    root = CommonUtils.requireNonEmpty(root),
    hookOfCreation = Objects.requireNonNull(hookOfCreation),
    hookOfUpdating = Objects.requireNonNull(hookOfUpdating),
    hookOfGet = Objects.requireNonNull(hookOfGet),
    hookOfList = Objects.requireNonNull(hookOfList),
    hookBeforeDelete = Objects.requireNonNull(hookBeforeDelete),
    hookOfPutActions = hookOfPutActions.toMap,
    hookOfFinalPutAction = hookOfFinalPutAction,
    hookOfDeleteActions = hookOfDeleteActions.toMap,
    hookOfFinalDeleteAction = hookOfFinalDeleteAction
  )

  protected def doBuild(root: String,
                        hookOfCreation: HookOfCreation[Creation, Res],
                        hookOfUpdating: HookOfUpdating[Updating, Res],
                        hookOfList: HookOfList[Res],
                        hookOfGet: HookOfGet[Res],
                        hookBeforeDelete: HookBeforeDelete,
                        hookOfPutActions: Map[String, HookOfAction[Res]],
                        hookOfFinalPutAction: Option[HookOfAction[Res]],
                        hookOfDeleteActions: Map[String, HookOfAction[ObjectKey]],
                        hookOfFinalDeleteAction: Option[HookOfAction[ObjectKey]]): Route

}

object RouteBuilder {

  def apply[Creation <: BasicCreation, Updating, Res <: Data: ClassTag]()(
    implicit store: DataStore,
    // normally, update request does not carry the name field,
    // Hence, the check of name have to be executed by format of creation
    // since it must have name field.
    rm: OharaJsonFormat[Creation],
    rm1: RootJsonFormat[Updating],
    rm2: RootJsonFormat[Res],
    executionContext: ExecutionContext): RouteBuilder[Creation, Updating, Res] =
    (root: String,
     hookOfCreation: HookOfCreation[Creation, Res],
     hookOfUpdating: HookOfUpdating[Updating, Res],
     hookOfList: HookOfList[Res],
     hookOfGet: HookOfGet[Res],
     hookBeforeDelete: HookBeforeDelete,
     hookOfPutActions: Map[String, HookOfAction[Res]],
     hookOfFinalPutAction: Option[HookOfAction[Res]],
     hookOfDeleteActions: Map[String, HookOfAction[ObjectKey]],
     hookOfFinalDeleteAction: Option[HookOfAction[ObjectKey]]) =>
      pathPrefix(root) {
        pathEnd {
          post(entity(as[Creation]) { creation =>
            complete(hookOfCreation(creation).flatMap(res => store.addIfAbsent(res)))
          }) ~
            get {
              parameterMap { params =>
                complete(
                  store
                    .values[Res]()
                    .flatMap(hookOfList(_))
                    .map(_.filter(_.matched(QueryRequest(params.filter {
                      // the empty stuff causes false always since there is nothing matched to "empty"
                      // hence, we remove them from parameters for careless users :)
                      case (key, value) => key.nonEmpty && value.nonEmpty
                    })))))
              }
            }
        } ~ path(Segment) { name =>
          parameter(GROUP_KEY ?) { groupOption =>
            val key =
              ObjectKey.of(
                rm.check(GROUP_KEY,
                         JsString(groupOption.getOrElse(com.island.ohara.client.configurator.v0.GROUP_DEFAULT)))
                  .value,
                rm.check(NAME_KEY, JsString(name)).value
              )
            get(complete(store.value[Res](key).flatMap(hookOfGet(_)))) ~
              delete(complete(
                hookBeforeDelete(key).map(_ => key).flatMap(store.remove[Res](_).map(_ => StatusCodes.NoContent)))) ~
              put(
                entity(as[Updating])(
                  update =>
                    complete(
                      store
                        .get[Res](key)
                        .flatMap(previous => hookOfUpdating(key = key, update = update, previous = previous))
                        .flatMap(store.add))))
          }
        }
      } ~ pathPrefix(root / Segment / Segment) {
        case (name, subName) =>
          parameterMap { params =>
            val key =
              ObjectKey.of(params.getOrElse(GROUP_KEY, com.island.ohara.client.configurator.v0.GROUP_DEFAULT), name)
            put {
              hookOfPutActions.get(subName).orElse(hookOfFinalPutAction) match {
                case None => routeToOfficialUrl(s"/$root/$subName")
                case Some(f) =>
                  complete {
                    store.value[Res](key).flatMap(res => f(res, subName, params)).map(_ => StatusCodes.Accepted)
                  }
              }
            } ~ delete {
              hookOfDeleteActions
                .get(subName)
                .orElse(hookOfFinalDeleteAction)
                .map(_(key, subName, params))
                .map(_.map(_ => StatusCodes.Accepted))
                .map(complete(_))
                .getOrElse(routeToOfficialUrl(s"/$root/$subName"))
            }
          }
    }
}
