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

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import forms.Choice
import models.DeclarationType
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.base.UnitSpec

import scala.concurrent.duration.FiniteDuration

class AppConfigSpec extends UnitSpec {

  private val environment = Environment.simple()

  private val validAppConfig: Config =
    ConfigFactory.parseString(
      """
        |urls.login="http://localhost:9949/auth-login-stub/gg-sign-in"
        |urls.loginContinue="http://localhost:9000/customs-declare-exports-frontend"
        |
        |urls.customsDeclarationsGoodsTakenOutOfEu="https://www.gov.uk/guidance/customs-declarations-for-goods-taken-out-of-the-eu"
        |urls.commodityCodes="https://www.gov.uk/guidance/finding-commodity-codes-for-imports-or-exports"
        |urls.relevantLicenses="https://www.gov.uk/starting-to-export/licences"
        |urls.serviceAvailability="https://www.gov.uk/guidance/customs-declaration-service-service-availability-and-issues"
        |urls.customsMovementsFrontend="http://url-to-movements-frontend/start"
        |
        |microservice.services.auth.host=localhostauth
        |google-analytics.token=N/A
        |google-analytics.host=localhostGoogle
        |
        |tracking-consent-frontend.gtm.container=a
        |
        |countryCodesCsvFilename=code-lists/mdg-country-codes.csv
        |countryCodesJsonFilename=code-lists/location-autocomplete-canonical-list.json
        |list-of-available-journeys="CRT,CAN,SUB"
        |list-of-available-declarations="STANDARD,SUPPLEMENTARY"
        |draft.timeToLive=30d
        |microservice.services.nrs.host=localhostnrs
        |microservice.services.nrs.port=7654
        |microservice.services.nrs.apikey=cds-exports
        |microservice.services.features.default=disabled
        |microservice.services.features.welsh-translation=false
        |microservice.services.features.use-improved-error-messages=true
        |microservice.services.auth.port=9988
        |microservice.services.customs-declare-exports.host=localhoste
        |microservice.services.customs-declare-exports.port=9875
        |microservice.services.customs-declare-exports.submit-declaration=/declaration
        |microservice.services.customs-declare-exports.declarations=/v2/declaration
        |microservice.services.customs-declare-exports.cancel-declaration=/cancellations
        |microservice.services.customs-declare-exports.fetch-notifications=/notifications
        |microservice.services.customs-declare-exports.fetch-submissions=/submissions
        |microservice.services.customs-declare-exports.fetch-submission-notifications=/submission-notifications
        |microservice.services.customs-declare-exports.fetch-ead=/ead
        |microservice.services.customs-declare-exports-movements.host=localhostm
        |microservice.services.customs-declare-exports-movements.port=9876
        |microservice.services.customs-declare-exports-movements.save-movement-uri=/save-movement-submission
        |mongodb.timeToLive=24h

      """.stripMargin
    )
  private val emptyAppConfig: Config = ConfigFactory.empty()
  val validServicesConfiguration = Configuration(validAppConfig)
  private val emptyServicesConfiguration = Configuration(emptyAppConfig)

  private def servicesConfig(conf: Configuration) = new ServicesConfig(conf)
  private def appConfig(conf: Configuration) = new AppConfig(conf, environment, servicesConfig(conf), "AppName")

  val validConfigService: AppConfig = appConfig(validServicesConfiguration)
  val emptyConfigService: AppConfig = appConfig(emptyServicesConfiguration)

  "The config" should {

    "have analytics token" in {
      validConfigService.analyticsToken must be("N/A")
    }

    "have analytics host" in {
      validConfigService.analyticsHost must be("localhostGoogle")
    }

    "have gtm container" in {
      validConfigService.gtmContainer must be("a")
    }

    "have auth URL" in {
      validConfigService.authUrl must be("http://localhostauth:9988")
    }

    "have login URL" in {
      validConfigService.loginUrl must be("http://localhost:9949/auth-login-stub/gg-sign-in")
    }

    "have customsDeclarationsGoodsTakenOutOfEu URL" in {
      validConfigService.customsDeclarationsGoodsTakenOutOfEuUrl must be(
        "https://www.gov.uk/guidance/customs-declarations-for-goods-taken-out-of-the-eu"
      )
    }

    "have commodityCodes URL" in {
      validConfigService.commodityCodesUrl must be("https://www.gov.uk/guidance/finding-commodity-codes-for-imports-or-exports")
    }

    "have relevantLicenses URL" in {
      validConfigService.relevantLicensesUrl must be("https://www.gov.uk/starting-to-export/licences")
    }

    "have serviceAvailability URL" in {
      validConfigService.serviceAvailabilityUrl must be("https://www.gov.uk/guidance/customs-declaration-service-service-availability-and-issues")
    }

    "have customsMovementsFrontend URL" in {
      validConfigService.customsMovementsFrontendUrl must be("http://url-to-movements-frontend/start")
    }

    "load the Choice options when list-of-available-journeys is defined" in {
      val choices = validConfigService.availableJourneys()
      choices.size must be(3)

      choices must contain(Choice.AllowedChoiceValues.CreateDec)
      choices must contain(Choice.AllowedChoiceValues.CancelDec)
      choices must contain(Choice.AllowedChoiceValues.Submissions)
    }

    "load the Declaration options when list-of-available-declarations is defined" in {
      val choices = validConfigService.availableDeclarations()
      choices.size must be(2)

      choices must contain(DeclarationType.STANDARD.toString)
      choices must contain(DeclarationType.SUPPLEMENTARY.toString)
    }

    "have login continue URL" in {
      validConfigService.loginContinueUrl must be("http://localhost:9000/customs-declare-exports-frontend")
    }

    "have language translation enabled field" in {
      validConfigService.languageTranslationEnabled must be(false)
    }

    "have improved error messages feature toggle set to false if not defined" in {
      emptyConfigService.isUsingImprovedErrorMessages must be(false)
    }

    "have improved error messages feature toggle set to true if defined" in {
      validConfigService.isUsingImprovedErrorMessages must be(true)
    }

    "have language map with English" in {
      validConfigService.languageMap.get("english").isDefined must be(true)
    }

    "have language map with Cymraeg" in {
      validConfigService.languageMap.get("cymraeg").isDefined must be(true)
    }

    "have customs declare exports" in {
      validConfigService.customsDeclareExports must be("http://localhoste:9875")
    }

    "have submit declaration URL" in {
      validConfigService.declarations must be("/v2/declaration")
    }

    "have cancel declaration URL" in {
      validConfigService.cancelDeclaration must be("/cancellations")
    }

    "have ead URL" in {
      validConfigService.fetchMrnStatus must be("/ead")
    }

    "have fetch notification URL" in {
      validConfigService.fetchNotifications must be("/notifications")
    }

    "have fetchSubmissions URL" in {
      validConfigService.fetchSubmissions must be("/submissions")
    }

    "have countryCodesJsonFilename" in {
      validConfigService.countryCodesJsonFilename must be("code-lists/location-autocomplete-canonical-list.json")
    }

    "have countriesCsvFilename" in {
      validConfigService.countriesCsvFilename must be("code-lists/mdg-country-codes.csv")
    }

    "have ttl lifetime" in {
      validConfigService.cacheTimeToLive must be(FiniteDuration(24, "h"))
    }

    "have draft lifetime" in {
      validConfigService.draftTimeToLive must be(FiniteDuration(30, TimeUnit.DAYS))
    }
  }

  "empty Choice options when list-of-available-journeys is not defined" in {
    emptyConfigService.availableJourneys().size must be(1)
    emptyConfigService.availableJourneys() must contain(Choice.AllowedChoiceValues.Submissions)
  }

  "empty Declaration type options when list-of-available-declarations is not defined" in {
    emptyConfigService.availableDeclarations().size must be(1)
    emptyConfigService.availableDeclarations() must contain(DeclarationType.STANDARD.toString)
  }

  "throw an exception when gtm.container is missing" in {
    intercept[Exception](emptyConfigService.gtmContainer).getMessage must be("Could not find config key 'tracking-consent-frontend.gtm.container'")
  }

  "throw an exception when google-analytics.host is missing" in {
    intercept[Exception](emptyConfigService.analyticsHost).getMessage must be("Missing configuration key: google-analytics.host")
  }

  "throw an exception when google-analytics.token is missing" in {
    intercept[Exception](emptyConfigService.analyticsToken).getMessage must be("Missing configuration key: google-analytics.token")
  }

  "throw an exception when auth.host is missing" in {
    intercept[Exception](emptyConfigService.authUrl).getMessage must be("Could not find config key 'auth.host'")
  }

  "throw an exception when urls.login is missing" in {
    intercept[Exception](emptyConfigService.loginUrl).getMessage must be("Missing configuration key: urls.login")
  }

  "throw an exception when urls.loginContinue is missing" in {
    intercept[Exception](emptyConfigService.loginContinueUrl).getMessage must be("Missing configuration key: urls.loginContinue")
  }

  "throw an exception when customs-declare-exports.host is missing" in {
    intercept[Exception](emptyConfigService.customsDeclareExports).getMessage must be("Could not find config key 'customs-declare-exports.host'")
  }

  "throw an exception when submit declaration uri is missing" in {
    intercept[Exception](emptyConfigService.declarations).getMessage must be(
      "Missing configuration for Customs Declarations Exports submit declaration URI"
    )
  }

  "throw an exception when cancel declaration uri is missing" in {
    intercept[Exception](emptyConfigService.cancelDeclaration).getMessage must be(
      "Missing configuration for Customs Declaration Export cancel declaration URI"
    )
  }

  "throw an exception when fetch mrn status uri is missing" in {
    intercept[Exception](emptyConfigService.fetchMrnStatus).getMessage must be(
      "Missing configuration for Customs Declaration Export fetch mrn status URI"
    )
  }

  "throw an exception when fetchSubmissions uri is missing" in {
    intercept[Exception](emptyConfigService.fetchSubmissions).getMessage must be(
      "Missing configuration for Customs Declaration Exports fetch submission URI"
    )
  }

  "throw an exception when fetch notifications uri is missing" in {
    intercept[Exception](emptyConfigService.fetchNotifications).getMessage must be(
      "Missing configuration for Customs Declarations Exports fetch notification URI"
    )
  }

  "throw an exception when countryCodesJsonFilename is missing" in {
    intercept[Exception](emptyConfigService.countryCodesJsonFilename).getMessage must be("Missing configuration key: countryCodesJsonFilename")
  }

  "throw an exception when countryCodesCsvFilename is missing" in {
    intercept[Exception](emptyConfigService.countriesCsvFilename).getMessage must be("Missing configuration key: countryCodesCsvFilename")
  }

}
