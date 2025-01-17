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

package config

import com.typesafe.config.ConfigFactory
import features.Feature
import play.api.Configuration
import unit.base.UnitSpec

class SfusConfigSpec extends UnitSpec {

  private def buildSfusConfig(sfusEnabled: Boolean = false, sfusKey: String = Feature.sfus.toString, sfusUploadKey: String = "sfusUpload") = {
    val config = Configuration(ConfigFactory.parseString(s"""
        |microservice.services.features.default=disabled
        |microservice.services.features.$sfusKey=${asConfigVal(sfusEnabled)}
        |urls.$sfusUploadKey=sfusLink
      """.stripMargin))

    new SfusConfig(new FeatureSwitchConfig(config), config)
  }

  private def asConfigVal(bool: Boolean): String = if (bool) "enabled" else "disabled"

  "SfusConfig on isSfusUploadEnabled" should {

    "return true" when {
      "sfus feature is enabled" in {
        buildSfusConfig(sfusEnabled = true).isSfusUploadEnabled mustBe true
      }
    }

    "return false" when {
      "sfus feature is disabled" in {
        buildSfusConfig().isSfusUploadEnabled mustBe false
      }

      "sfus feature config key doesn't exist" in {
        buildSfusConfig(sfusEnabled = true, sfusKey = "WRONG").isSfusUploadEnabled mustBe false
      }
    }
  }

  "SfusConfig on isSfusUploadEnabled" should {

    "return the correct sfusUpload url if present" in {
      buildSfusConfig().sfusUploadLink mustBe "sfusLink"
    }

    "throw an exception if url is missing" in {
      intercept[IllegalStateException] {
        buildSfusConfig(sfusUploadKey = "WRONG")
      }
    }
  }

}
