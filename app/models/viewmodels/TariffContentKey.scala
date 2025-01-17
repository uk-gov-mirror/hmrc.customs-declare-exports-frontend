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

package models.viewmodels

import config.AppConfig
import models.DeclarationType.DeclarationType
import play.api.i18n.Messages

case class TariffContentKey(key: String) {
  def getTextKey(): String = s"$key.text"
  def getLinkText(idx: Int)(implicit messages: Messages): String = messages(s"$key.linkText.${idx}")
  def getUrl(idx: Int)(implicit appConfig: AppConfig) = appConfig.tariffGuideUrl(s"urls.$key.${idx}")
}
