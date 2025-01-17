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
import controllers.util.DeclarationHolderHelper.cachedHolders
import forms.common.YesNoAnswer
import forms.common.YesNoAnswer.YesNoAnswers

import javax.inject.Inject
import models.DeclarationType.{SIMPLIFIED, STANDARD, SUPPLEMENTARY}
import models.Mode
import models.requests.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.declarationHolder.declaration_holder_summary

class DeclarationHolderController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  declarationHolderPage: declaration_holder_summary
) extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val holders = cachedHolders
    if (holders.isEmpty) navigator.continueTo(mode, nextPageWhenNoHolders)
    else Ok(declarationHolderPage(mode, addAnotherYesNoForm.withSubmissionErrors(), holders))
  }

  def submitForm(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val holders = cachedHolders
    addAnotherYesNoForm
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[YesNoAnswer]) => BadRequest(declarationHolderPage(mode, formWithErrors, holders)),
        validYesNo =>
          validYesNo.answer match {
            case YesNoAnswers.yes => navigator.continueTo(mode, routes.DeclarationHolderAddController.displayPage)
            case YesNoAnswers.no  => navigator.continueTo(mode, nextPage)
        }
      )
  }

  private val addAnotherYesNoForm: Form[YesNoAnswer] =
    YesNoAnswer.form(errorKey = "declaration.declarationHolders.add.another.empty")

  private def nextPageWhenNoHolders(implicit request: JourneyRequest[_]): Mode => Call =
    if (request.declarationType == SIMPLIFIED) routes.DeclarationHolderAddController.displayPage
    else routes.DeclarationHolderRequiredController.displayPage

  private def nextPage(implicit request: JourneyRequest[_]): Mode => Call =
    request.declarationType match {
      case SUPPLEMENTARY | STANDARD => routes.OriginationCountryController.displayPage
      case _                        => routes.DestinationCountryController.displayPage
    }
}
