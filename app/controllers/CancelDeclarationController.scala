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

package controllers

import connectors.CustomsDeclareExportsConnector
import controllers.actions.{AuthAction, VerifiedEmailAction}
import forms.CancelDeclaration
import forms.CancelDeclaration._
import handlers.ErrorHandler
import metrics.ExportsMetrics
import metrics.MetricIdentifiers._
import models.requests.AuthenticatedRequest
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.audit.{AuditService, AuditTypes}
import services.audit.EventData._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{cancel_declaration, cancellation_confirmation_page}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CancelDeclarationController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  customsDeclareExportsConnector: CustomsDeclareExportsConnector,
  errorHandler: ErrorHandler,
  exportsMetrics: ExportsMetrics,
  mcc: MessagesControllerComponents,
  auditService: AuditService,
  cancelDeclarationPage: cancel_declaration,
  cancelConfirmationPage: cancellation_confirmation_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  def displayPage(): Action[AnyContent] = (authenticate andThen verifyEmail) { implicit request =>
    Ok(cancelDeclarationPage(CancelDeclaration.form))
  }

  def onSubmit(): Action[AnyContent] = (authenticate andThen verifyEmail).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[CancelDeclaration]) => Future.successful(BadRequest(cancelDeclarationPage(formWithErrors))),
        form => {
          auditService.auditAllPagesDeclarationCancellation(form)
          val context = exportsMetrics.startTimer(cancelMetric)
          customsDeclareExportsConnector.createCancellation(form) andThen {
            case Failure(exception) =>
              logger.error(s"Error response from backend $exception")
              auditService.audit(AuditTypes.Cancellation, auditData(form, Failure.toString))
            case Success(_) =>
              auditService.audit(AuditTypes.Cancellation, auditData(form, Success.toString))
              exportsMetrics.incrementCounter(cancelMetric)
              context.stop()
          } map (_ => Ok(cancelConfirmationPage()))
        }
      )
  }

  private def auditData(form: CancelDeclaration, result: String)(implicit request: AuthenticatedRequest[_]): Map[String, String] =
    Map(
      eori.toString -> request.user.eori,
      lrn.toString -> form.functionalReferenceId.value,
      mrn.toString -> form.mrn,
      changeReason.toString -> form.changeReason,
      changeDescription.toString -> form.statementDescription,
      submissionResult.toString -> result
    )
}
