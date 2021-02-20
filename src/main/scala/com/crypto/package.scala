package com

import io.circe.generic.extras.Configuration

package object crypto {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
