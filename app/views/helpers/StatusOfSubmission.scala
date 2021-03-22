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

package views.helpers

import models.declaration.notifications.Notification
import models.declaration.submissions.SubmissionStatus.SubmissionStatus
import play.api.i18n.Messages

object StatusOfSubmission {

  def asText(status: SubmissionStatus)(implicit messages: Messages): String =
    messages(s"submission.status.${status.toString}")

  def asText(notification: Notification)(implicit messages: Messages): String =
    asText(notification.status)

  def toLowerCase(notification: Notification)(implicit messages: Messages): String =
    asText(notification).toLowerCase
}
