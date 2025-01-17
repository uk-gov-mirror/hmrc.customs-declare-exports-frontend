/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.actions.{AuthAction, JourneyAction, VerifiedEmailAction}
import controllers.navigation.Navigator
import forms.common.YesNoAnswer
import forms.common.YesNoAnswer.YesNoAnswers
import forms.declaration.DeclarationAdditionalActors

import javax.inject.Inject
import models.declaration.DeclarationAdditionalActorsData
import models.requests.JourneyRequest
import models.{ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ListItem
import views.html.declaration.additionalActors.additional_actors_remove

import scala.concurrent.{ExecutionContext, Future}

class AdditionalActorsRemoveController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  removePage: additional_actors_remove
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode, id: String): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    findActor(id) match {
      case Some(actor) => Ok(removePage(mode, id, actor, removeYesNoForm.withSubmissionErrors()))
      case _           => navigator.continueTo(mode, routes.AdditionalActorsSummaryController.displayPage)
    }
  }

  def submitForm(mode: Mode, id: String): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    findActor(id) match {
      case Some(actor) =>
        removeYesNoForm
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[YesNoAnswer]) => Future.successful(BadRequest(removePage(mode, id, actor, formWithErrors))),
            formData => {
              formData.answer match {
                case YesNoAnswers.yes =>
                  updateExportsCache(actor)
                    .map(_ => navigator.continueTo(mode, routes.AdditionalActorsSummaryController.displayPage))
                case YesNoAnswers.no =>
                  Future.successful(navigator.continueTo(mode, routes.AdditionalActorsSummaryController.displayPage))
              }
            }
          )
      case _ => Future.successful(navigator.continueTo(mode, routes.AdditionalActorsSummaryController.displayPage))
    }
  }

  private def removeYesNoForm: Form[YesNoAnswer] = YesNoAnswer.form(errorKey = "declaration.additionalActors.remove.empty")

  private def findActor(id: String)(implicit request: JourneyRequest[AnyContent]): Option[DeclarationAdditionalActors] =
    ListItem.findById(id, request.cacheModel.parties.declarationAdditionalActorsData.map(_.actors).getOrElse(Seq.empty))

  private def updateExportsCache(
    itemToRemove: DeclarationAdditionalActors
  )(implicit request: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] = {
    val updatedActors = request.cacheModel.parties.declarationAdditionalActorsData.map(_.actors).getOrElse(Seq.empty).filterNot(_ == itemToRemove)
    val updatedParties = request.cacheModel.parties.copy(declarationAdditionalActorsData = Some(DeclarationAdditionalActorsData(updatedActors)))
    updateExportsDeclarationSyncDirect(model => model.copy(parties = updatedParties))
  }
}
