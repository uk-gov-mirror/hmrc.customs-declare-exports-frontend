/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.declaration

import controllers.actions.{AuthAction, JourneyAction}
import controllers.navigation.Navigator
import controllers.util.MultipleItemsHelper.remove
import controllers.util._
import forms.declaration.NactCode
import forms.declaration.NactCode.{form, nactCodeLimit}
import handlers.ErrorHandler
import javax.inject.Inject
import models.requests.JourneyRequest
import models.{ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.declaration.nact_codes

import scala.concurrent.{ExecutionContext, Future}

class NactCodeController @Inject()(
  authenticate: AuthAction,
  journeyType: JourneyAction,
  errorHandler: ErrorHandler,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  nactCodesPage: nact_codes
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable {

  def displayPage(mode: Mode, itemId: String): Action[AnyContent] = (authenticate andThen journeyType) { implicit request =>
    val nactCodes = request.cacheModel.itemBy(itemId).map(_.nactCodes).getOrElse(Seq.empty)
    Ok(nactCodesPage(mode, itemId, form(), nactCodes))
  }

  def submitForm(mode: Mode, itemId: String): Action[AnyContent] = (authenticate andThen journeyType).async { implicit request =>
    val actionTypeOpt = FormAction.bindFromRequest()
    val boundForm = form().bindFromRequest()
    val nactCodes = request.cacheModel.itemBy(itemId).map(_.nactCodes).getOrElse(Seq.empty)
    actionTypeOpt match {
      case Add                             => addItem(mode, itemId, boundForm, nactCodes)
      case Remove(values)                  => removeItem(mode, itemId, values, boundForm, nactCodes)
      case SaveAndContinue | SaveAndReturn => saveAndContinue(mode, itemId, boundForm, nactCodes)
      case _                               => errorHandler.displayErrorPage()
    }
  }

  private def addItem(mode: Mode, itemId: String, boundForm: Form[NactCode], cachedData: Seq[NactCode])(
    implicit request: JourneyRequest[AnyContent]
  ): Future[Result] =
    MultipleItemsHelper
      .add(boundForm, cachedData, nactCodeLimit)
      .fold(
        formWithErrors => Future.successful(BadRequest(nactCodesPage(mode, itemId, formWithErrors, cachedData))),
        updatedCache =>
          updateExportsCache(itemId, updatedCache)
            .map(_ => Redirect(controllers.declaration.routes.NactCodeController.displayPage(mode, itemId)))
      )

  private def removeItem(mode: Mode, itemId: String, values: Seq[String], boundForm: Form[NactCode], items: Seq[NactCode])(
    implicit request: JourneyRequest[AnyContent]
  ): Future[Result] = {
    val itemToRemove = items.find(_.nactCode.equals(values.head))
    val updatedCache = remove(items, itemToRemove.contains(_: NactCode))
    updateExportsCache(itemId, updatedCache)
      .map(_ => Ok(nactCodesPage(mode, itemId, boundForm.discardingErrors, updatedCache)))
  }

  private def saveAndContinue(mode: Mode, itemId: String, boundForm: Form[NactCode], cachedData: Seq[NactCode])(
    implicit request: JourneyRequest[AnyContent]
  ): Future[Result] =
    MultipleItemsHelper
      .saveAndContinue(boundForm, cachedData, isMandatory = false, nactCodeLimit)
      .fold(
        formWithErrors => Future.successful(BadRequest(nactCodesPage(mode, itemId, formWithErrors, cachedData))),
        updatedCache =>
          if (updatedCache != cachedData)
            updateExportsCache(itemId, updatedCache)
              .map(_ => navigator.continueTo(nextPage(mode, itemId)))
          else
            Future
              .successful(navigator.continueTo(nextPage(mode, itemId)))
      )

  private def nextPage(mode: Mode, itemId: String) =
    controllers.declaration.routes.ItemTypeController.displayPage(mode, itemId)

  private def updateExportsCache(itemId: String, updatedCache: Seq[NactCode])(
    implicit r: JourneyRequest[AnyContent]
  ): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(model => model.updatedItem(itemId, _.copy(nactCodes = updatedCache.toList)))

}