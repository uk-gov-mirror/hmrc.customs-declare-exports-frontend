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
import forms.common.YesNoAnswer.YesNoAnswers.{no, yes}
import forms.declaration.RepresentativeAgent

import javax.inject.Inject
import models.declaration.RepresentativeDetails
import models.requests.JourneyRequest
import models.{ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.representative_details_agent

import scala.concurrent.{ExecutionContext, Future}

class RepresentativeAgentController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  override val exportsCacheService: ExportsCacheService,
  mcc: MessagesControllerComponents,
  representativeAgentPage: representative_details_agent
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val frm = RepresentativeAgent.form().withSubmissionErrors()
    request.cacheModel.parties.representativeDetails.flatMap(_.representingOtherAgent) match {
      case Some(data) => Ok(representativeAgentPage(mode, frm.fill(RepresentativeAgent(data))))
      case _          => Ok(representativeAgentPage(mode, frm))
    }
  }

  def submitForm(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    RepresentativeAgent
      .form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[RepresentativeAgent]) => Future.successful(BadRequest(representativeAgentPage(mode, formWithErrors))),
        validRepresentativeDetails =>
          updateCache(validRepresentativeDetails).map(_ => navigator.continueTo(mode, nextPage(validRepresentativeDetails)))
      )
  }

  private def nextPage(formData: RepresentativeAgent): Mode => Call =
    if (formData.representingAgent == yes) controllers.declaration.routes.RepresentativeEntityController.displayPage
    else
      controllers.declaration.routes.RepresentativeStatusController.displayPage

  private def updateCache(formData: RepresentativeAgent)(implicit request: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect { model =>
      val representativeDetails: RepresentativeDetails = model.parties.representativeDetails.getOrElse(RepresentativeDetails())
      val updatedParties =
        model.parties.copy(
          representativeDetails = Some(
            representativeDetails.copy(
              representingOtherAgent = Some(formData.representingAgent),
              details = if (formData.representingAgent == no) None else representativeDetails.details
            )
          )
        )
      model.copy(parties = updatedParties)
    }
}
