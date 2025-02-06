import toni.blahaj.setup.implementation
import toni.blahaj.setup.include

plugins {
	id("toni.blahaj")
}

blahaj {
	config {
		// yarn()
		// versionedAccessWideners()
	}
	setup {
		txnilib("1.0.22")
		forgeConfig()

		deps.implementation ("systems.manifold:manifold-rt:2024.1.54")
		deps.testImplementation("junit:junit:4.12")
		deps.annotationProcessor("systems.manifold", "manifold", "2024.1.54")
		deps.testAnnotationProcessor("systems.manifold", "manifold", "2024.1.54")

		// Bundled
		deps.compileOnly(deps.annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")!!)
		deps.implementation(deps.include("io.github.llamalad7:mixinextras-$loader:0.4.1")!!)

		// Required
		deps.modImplementation("fuzs.puzzleslib:puzzleslib-$loader:${property("puzzleslib")}")


		// Optional / Compats
		// TODO: Appleskin, Ars Nouveau, Iron's Spellbooks, maybe RPGMana
		//if (project.hasProperty("overflowingbars")) { deps.modImplementation("maven.modrinth:overflowing-bars:v${property("overflowingbars")}-$mc-$loader") }


		// Publishing, required deps
		addRequiredMod("puzzleslib")
			.modrinth("puzzles-lib") // override with Modrinth URL slug
			.addPlatform("1.21.1-neoforge", "v21.1.27-1.21.1-Fabric") { required() }
			.addPlatform("1.21.1-fabric", "v21.1.27-1.21.1-Fabric") { required() }
			.addPlatform("1.20.1-forge", "v8.1.25-1.20.1-Forge") { required() }
			.addPlatform("1.20.1-fabric", "v8.1.25-1.20.1-Fabric") { required() }
	}
}

dependencies {
	implementation ("systems.manifold:manifold-rt:2024.1.54")
	testImplementation("junit:junit:4.12")
	annotationProcessor("systems.manifold", "manifold", "2024.1.54")
	testAnnotationProcessor("systems.manifold", "manifold", "2024.1.54")
}

repositories {
	maven("https://maven.ladysnake.org/releases")
	maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
	maven("https://maven.kosmx.dev/")
	maven {
		url = uri("https://code.redspace.io/releases")
		content {
			includeGroup("io.redspace")
		}
	}
	maven {
		url = uri("https://code.redspace.io/snapshots")
		content {
			includeGroup("io.redspace")
		}
	}
}