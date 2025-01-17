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

import javax.inject.Inject
import models.Mode
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.previousDocuments.previous_documents_summary

class PreviousDocumentsSummaryController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  override val exportsCacheService: ExportsCacheService,
  mcc: MessagesControllerComponents,
  previousDocumentsSummary: previous_documents_summary
) extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val form = anotherYesNoForm.withSubmissionErrors()
    request.cacheModel.previousDocuments.map(_.documents) match {
      case Some(documents) if documents.nonEmpty => Ok(previousDocumentsSummary(mode, form, documents))
      case _                                     => navigator.continueTo(mode, controllers.declaration.routes.PreviousDocumentsController.displayPage)
    }
  }

  def submit(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val previousDocuments = request.cacheModel.previousDocuments.map(_.documents).getOrElse(Seq.empty)

    anotherYesNoForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(previousDocumentsSummary(mode, formWithErrors, previousDocuments)),
        validAnswer =>
          validAnswer.answer match {
            case YesNoAnswers.yes => navigator.continueTo(mode, controllers.declaration.routes.PreviousDocumentsController.displayPage)
            case YesNoAnswers.no  => navigator.continueTo(mode, controllers.declaration.routes.ItemsSummaryController.displayAddItemPage)
        }
      )
  }

  private def anotherYesNoForm: Form[YesNoAnswer] = YesNoAnswer.form(errorKey = "declaration.previousDocuments.add.another.empty")
}
