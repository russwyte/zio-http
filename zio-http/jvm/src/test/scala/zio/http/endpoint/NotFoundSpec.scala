/*
 * Copyright 2021 - 2023 Sporta Technologies PVT LTD & the ZIO HTTP contributors.
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

package zio.http.endpoint

import zio._
import zio.test._

import zio.http.Method._
import zio.http._
import zio.http.codec._

object NotFoundSpec extends ZIOHttpSpec {
  def spec = suite("NotFoundSpec")(
    test("on wrong path") {
      check(Gen.int) { userId =>
        val testRoutes = test404(
          Routes(
            Endpoint(GET / "users" / int("userId"))
              .out[String]
              .implementHandler {
                Handler.fromFunction { userId =>
                  s"path(users, $userId)"
                }
              },
            Endpoint(GET / "users" / int("userId") / "posts" / int("postId"))
              .query(HttpCodec.query[String]("name"))
              .out[String]
              .implementHandler {
                Handler.fromFunction { case (userId, postId, name) =>
                  s"path(users, $userId, posts, $postId) query(name=$name)"
                }
              },
          ),
        ) _
        testRoutes(s"/user/$userId", Method.GET) &&
        testRoutes(s"/users/$userId/wrong", Method.GET)
      }
    },
    test("on wrong method") {
      check(Gen.int, Gen.int, Gen.alphaNumericString) { (userId, postId, name) =>
        val testRoutes = test404(
          Routes(
            Endpoint(GET / "users" / int("userId"))
              .out[String]
              .implementHandler {
                Handler.fromFunction { userId =>
                  s"path(users, $userId)"
                }
              },
            Endpoint(GET / "users" / int("userId") / "posts" / int("postId"))
              .query(HttpCodec.query[String]("name"))
              .out[String]
              .implementHandler {
                Handler.fromFunction { case (userId, postId, name) =>
                  s"path(users, $userId, posts, $postId) query(name=$name)"
                }
              },
          ),
        ) _
        testRoutes(s"/users/$userId", Method.POST) &&
        testRoutes(s"/users/$userId/posts/$postId?name=$name", Method.PUT)
      }
    },
  )

  def test404[R](service: Routes[R, Nothing])(
    url: String,
    method: Method,
  ): ZIO[Scope & R, Response, TestResult] = {
    val request = Request(method = method, url = URL.decode(url).toOption.get)
    for {
      response <- service.runZIO(request)
      result = response.status == Status.NotFound
    } yield assertTrue(result)
  }
}
