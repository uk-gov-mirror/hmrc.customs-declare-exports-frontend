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

package controllers

import connectors.CustomsDeclareExportsConnector
import controllers.actions.AuthAction
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.model.RejectionReason
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.rejected_notification_errors

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RejectedNotificationsController @Inject()(
  authenticate: AuthAction,
  customsDeclareExportsConnector: CustomsDeclareExportsConnector,
  mcc: MessagesControllerComponents,
  rejectedNotificationPage: rejected_notification_errors
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(id: String): Action[AnyContent] = authenticate.async { implicit request =>
    customsDeclareExportsConnector.findSubmission(id).flatMap {
      case Some(submission) =>
        customsDeclareExportsConnector.findNotifications(id).map { notifications =>
          Ok(rejectedNotificationPage(submission, RejectionReason.fromNotifications(notifications)))
        }
      case None => Future.successful(Redirect(routes.SubmissionsController.displayListOfSubmissions()))
    }
  }
}