/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.util.MultipleItemsHelper.saveAndContinue
import controllers.util.{FormAction, Remove}
import forms.common.YesNoAnswer
import forms.common.YesNoAnswer.{form, YesNoAnswers}
import forms.declaration.Seal
import handlers.ErrorHandler
import javax.inject.Inject
import models.Mode
import models.declaration.{Container, TransportInformation}
import models.requests.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.declaration.{seal_add, seal_remove, seal_summary}

import scala.concurrent.{ExecutionContext, Future}

class SealController @Inject()(
  authenticate: AuthAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  errorHandler: ErrorHandler,
  override val exportsCacheService: ExportsCacheService,
  mcc: MessagesControllerComponents,
  addPage: seal_add,
  removePage: seal_remove,
  summaryPage: seal_summary
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable {

  def displayAddSeal(mode: Mode, containerId: String): Action[AnyContent] = (authenticate andThen journeyType) { implicit request =>
    Ok(addPage(mode, Seal.form(), containerId))
  }

  def submitAddSeal(mode: Mode, containerId: String): Action[AnyContent] = (authenticate andThen journeyType).async { implicit request =>
    val boundForm = Seal.form().bindFromRequest()

    request.cacheModel.containerBy(containerId) match {
      case Some(container) =>
        saveSeal(mode, boundForm, container)
      case _ => errorHandler.displayErrorPage()
    }
  }

  def displaySealSummary(mode: Mode, containerId: String): Action[AnyContent] = (authenticate andThen journeyType) { implicit request =>
    Ok(summaryPage(mode, YesNoAnswer.form(), request.cacheModel.containerBy(containerId)))
  }

  def submitSummaryAction(mode: Mode, containerId: String): Action[AnyContent] =
    (authenticate andThen journeyType).async { implicit request =>
      FormAction.bindFromRequest() match {
        case Remove(values) => confirmRemoveSeal(containerId, sealId(values), mode)
        case _              => addSealAnswer(mode, containerId)
      }
    }

  def displaySealRemove(mode: Mode, containerId: String, sealId: String): Action[AnyContent] =
    (authenticate andThen journeyType) { implicit request =>
      Ok(removePage(mode, YesNoAnswer.form(), containerId, sealId))
    }

  def submitSealRemove(mode: Mode, containerId: String, sealId: String): Action[AnyContent] =
    (authenticate andThen journeyType).async { implicit request =>
      removeSealAnswer(mode, containerId, sealId)
    }

  private def sealId(values: Seq[String]): String = values.headOption.getOrElse("")

  private def addSealAnswer(mode: Mode, containerId: String)(implicit request: JourneyRequest[AnyContent]) =
    form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[YesNoAnswer]) =>
          Future.successful(BadRequest(summaryPage(mode, formWithErrors, request.cacheModel.containerBy(containerId)))),
        formData =>
          formData.answer match {
            case YesNoAnswers.yes =>
              Future.successful(navigator.continueTo(routes.SealController.displayAddSeal(mode, containerId)))
            case YesNoAnswers.no =>
              Future
                .successful(navigator.continueTo(routes.TransportContainerController.displayContainerSummary(mode)))
        }
      )

  private def removeSealAnswer(mode: Mode, containerId: String, sealId: String)(implicit request: JourneyRequest[AnyContent]) =
    form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[YesNoAnswer]) => Future.successful(BadRequest(removePage(mode, formWithErrors, containerId, sealId))),
        formData =>
          formData.answer match {
            case YesNoAnswers.yes =>
              removeSeal(containerId, sealId, mode)
            case YesNoAnswers.no =>
              Future
                .successful(navigator.continueTo(routes.SealController.displaySealSummary(mode, containerId)))
        }
      )

  private def confirmRemoveSeal(containerId: String, sealId: String, mode: Mode)(implicit request: JourneyRequest[AnyContent]) =
    Future.successful(navigator.continueTo(routes.SealController.displaySealRemove(mode, containerId, sealId)))

  private def removeSeal(containerId: String, sealId: String, mode: Mode)(implicit request: JourneyRequest[AnyContent]) = {
    val result =
      request.cacheModel.containerBy(containerId).map(c => c.copy(seals = c.seals.filterNot(_.id == sealId))) match {
        case Some(container) => updateCache(container)
        case _               => Future.successful(None)
      }
    result.map(_ => navigator.continueTo(routes.SealController.displaySealSummary(mode, containerId)))
  }

  private def saveSeal(mode: Mode, boundForm: Form[Seal], cachedContainer: Container)(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    saveAndContinue(boundForm, cachedContainer.seals, isMandatory = true, Seal.sealsAllowed).fold(
      formWithErrors => Future.successful(BadRequest(addPage(mode, formWithErrors, cachedContainer.id))),
      updatedCache =>
        if (updatedCache != cachedContainer.seals) updateCache(cachedContainer.copy(seals = updatedCache)).map { _ =>
          navigator.continueTo(routes.SealController.displaySealSummary(mode, cachedContainer.id))
        } else
          Future.successful(navigator.continueTo(routes.SealController.displaySealSummary(mode, cachedContainer.id)))
    )

  private def updateCache(updatedContainer: Container)(implicit req: JourneyRequest[AnyContent]) =
    updateExportsDeclarationSyncDirect(model => model.addOrUpdateContainer(updatedContainer))

}
