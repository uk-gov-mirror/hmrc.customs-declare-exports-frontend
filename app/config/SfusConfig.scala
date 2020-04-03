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

package config

import features.{Feature, FeatureStatus}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class SfusConfig @Inject()(featureSwitchConfig: FeatureSwitchConfig, servicesConfig: ServicesConfig) {

  val sfusLink = servicesConfig.baseUrl("cds-file-upload-frontend") + servicesConfig.getConfString(
    "cds-file-upload-frontend.start",
    throw new IllegalStateException("Missing configuration for CDS File Upload frontend start")
  )

  val isSfusEnabled = featureSwitchConfig.featureStatus(Feature.sfus) == FeatureStatus.enabled
}