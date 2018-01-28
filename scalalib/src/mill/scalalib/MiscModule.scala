package mill
package scalalib

import ammonite.ops.{Path, RelPath}
import mill.define.Cross.Resolver
import mill.define.{Cross, Task}
import mill.eval.{PathRef, Result}
import mill.util.Loose.Agg
object CrossModuleBase{
  def scalaVersionPaths(scalaVersion: String, f: String => Path) = {
    for(segments <- scalaVersion.split('.').inits.filter(_.nonEmpty))
    yield PathRef(f(segments.mkString(".")))
  }
}
trait CrossModuleBase extends mill.Module{
  def crossScalaVersion: String
  override def basePath = super.basePath / ammonite.ops.up
  implicit def crossSbtModuleResolver: Resolver[CrossModuleBase] = new Resolver[CrossModuleBase]{
    def resolve[V <: CrossModuleBase](c: Cross[V]): V = {
      crossScalaVersion.split('.')
        .inits
        .takeWhile(_.length > 1)
        .flatMap( prefix =>
          c.items.map(_._2).find(_.crossScalaVersion.split('.').startsWith(prefix))
        )
        .collectFirst{case x => x}
        .getOrElse(
          throw new Exception(
            s"Unable to find compatible cross version between $crossScalaVersion and "+
              c.items.map(_._2.crossScalaVersion).mkString(",")
          )
        )
    }
  }
}

trait CrossScalaModule extends ScalaModule with CrossModuleBase{ outer =>
  override def sources = T.input{
    super.sources() ++
    CrossModuleBase.scalaVersionPaths(crossScalaVersion, s => basePath / s"src-$s" )

  }

  trait Tests extends super.Tests {
    override def sources = T.input{
      super.sources() ++
      CrossModuleBase.scalaVersionPaths(crossScalaVersion, s => basePath / s"src-$s" )
    }
  }
}

trait SbtModule extends ScalaModule { outer =>
  override def sources = T.input{
    Agg(
      PathRef(basePath / 'src / 'main / 'scala),
      PathRef(basePath / 'src / 'main / 'java)
    )
  }
  override def resources = T.input{ Agg(PathRef(basePath / 'src / 'main / 'resources)) }
  trait Tests extends super.Tests {
    override def basePath = outer.basePath
    override def sources = T.input{
      Agg(
        PathRef(basePath / 'src / 'test / 'scala),
        PathRef(basePath / 'src / 'test / 'java)
      )
    }
    override def resources = T.input{ Agg(PathRef(basePath / 'src / 'test / 'resources)) }
  }
}

trait CrossSbtModule extends SbtModule with CrossModuleBase{ outer =>

  def scalaVersion = crossScalaVersion
  override def sources = T.input{
    super.sources() ++
    CrossModuleBase.scalaVersionPaths(
      crossScalaVersion,
      s => basePath / 'src / 'main / s"scala-$s"
    )

  }
  trait Tests extends super.Tests {
    override def basePath = outer.basePath
    override def sources = T.input{
      super.sources() ++
      CrossModuleBase.scalaVersionPaths(
        crossScalaVersion,
        s => basePath / 'src / 'main / s"scala-$s"
      )
    }
  }
}


