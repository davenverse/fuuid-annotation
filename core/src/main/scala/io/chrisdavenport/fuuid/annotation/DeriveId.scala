package io.chrisdavenport.fuuid.annotation

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.reflect.macros.whitebox

/**
 * This annotation can be used on any kind of object to automatically
 * create an inner `Id` tagged `FUUID` type with convenient methods for
 * its creation. It also provides implicit instances for cats' `Hash`,
 * `Order` and `Show` type-classes. All these instances are available
 * in the enclosing object.
 *
 * @example For an object named `User` {{{
 * object User {
 *
 *    trait IdTag
 *
 *    type Id = shapeless.tag.@@[FUUID, IdTag]
 *
 *    object Id {
 *
 *        //Creates a new `Id` from a `FUUID`
 *        def apply(fuuid: FUUID): User.Id = ???
 *
 *        //Creates a new `Id` from an `UUID` literal. This method
 *        //uses a macro to compile check the literal value
 *        def apply(s: String): User.Id = ???
 *
 *
 *        //Creates a random `Id` wrapped in an `F`
 *        def random[F[_]: cats.effect.Sync]: F[User.Id] = ???
 *
 *        object Unsafe {
 *
 *          //Creates an unwrapped random `Id`
 *          def random: User.Id = ???
 *
 *        }
 *
 *    }
 *
 *    implicit val IdHashOrderShowInstances: Hash[User.Id]
 *      with Order[User.Id] with Show[User.Id] = ???
 *
 * }
 * }}}
 */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class DeriveId extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro DeriveIdMacros.impl
}

object DeriveIdMacros {

  def fuuidLiteral(c: whitebox.Context)(s: c.Expr[String]): c.Tree = {
    import c.universe._

    q"""
      @SuppressWarnings(Array("org.wartremover.warts.Throw"))
      val id = ${c.prefix}.apply(_root_.io.chrisdavenport.fuuid.FUUID.fuuid($s))
      id
    """
  }

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Any",
      "org.wartremover.warts.Nothing",
      "org.wartremover.warts.PublicInference"
    ))
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val (mods, name, parents, body) = (annottees map (_.tree)).headOption collect {
      case q"$mods object $name extends ..$parents { ..$body }" => (mods, name, parents, body)
    } getOrElse c.abort(c.enclosingPosition, "@DeriveId can only be used with objects")

    c.Expr[Any](q"""
      $mods object $name extends ..$parents {

        trait IdTag
      
        type Id = _root_.shapeless.tag.@@[_root_.io.chrisdavenport.fuuid.FUUID, IdTag]
    
        @SuppressWarnings(Array(
          "org.wartremover.warts.Overloading",
          "org.wartremover.warts.PublicInference"
        ))
        object Id {
    
          def apply(fuuid: _root_.io.chrisdavenport.fuuid.FUUID): $name.Id =
            _root_.shapeless.tag[IdTag][_root_.io.chrisdavenport.fuuid.FUUID](fuuid)
    
          def apply(s: String): $name.Id =
            macro _root_.io.chrisdavenport.fuuid.annotation.DeriveIdMacros.fuuidLiteral
    
          def random[F[_]: _root_.cats.effect.Sync]: F[$name.Id] =
            _root_.cats.effect.Sync[F].map(_root_.io.chrisdavenport.fuuid.FUUID.randomFUUID[F])(apply)
    
          object Unsafe {

            def random: $name.Id =
              Id(_root_.io.chrisdavenport.fuuid.FUUID.randomFUUID[_root_.cats.effect.IO].unsafeRunSync())
        
          }
          
        }

        @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
        implicit val IdHashOrderShowInstances: _root_.cats.Hash[$name.Id] with _root_.cats.Order[$name.Id] with _root_.cats.Show[$name.Id] =
          new _root_.cats.Hash[$name.Id] with _root_.cats.Order[$name.Id] with _root_.cats.Show[$name.Id] {
            override def show(t: $name.Id): String = t.show
            override def eqv(x: $name.Id, y: $name.Id): Boolean = x.eqv(y)
            override def hash(x: $name.Id): Int = x.hashCode
            override def compare(x: $name.Id, y: $name.Id): Int = x.compare(y)
          }

        ..$body
      }
    """)
  }
}
