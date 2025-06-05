import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.annotationProcessor
import org.gradle.kotlin.dsl.modRuntimeOnly
import toni.blahaj.setup.*

plugins {
	id("toni.blahaj")
}

allprojects {
	configurations.all {

		/*
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
		exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
		*/
		exclude(group = "toni.txnilib")

	}
}

blahaj {
	config {
		// yarn()
		 versionedAccessWideners()
	}
	setup {
		//forgeConfig()

		// Bundled
		deps.compileOnly(deps.annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")!!)
		deps.implementation(deps.include("io.github.llamalad7:mixinextras-$loader:0.4.1")!!)

		// Required
		deps.modImplementation("fuzs.puzzleslib:puzzleslib-$loader:${property("puzzleslib")}")

		// Integrations
		// TODO: Farmer's Delight
		// TODO: Mana: Ars Nouveau, Iron's Spellbooks, RPGMana, Mana Attributes
		// Frostiful, Scorchful
		if (project.hasProperty("thermoo")) { deps.modImplementation("maven.modrinth:thermoo:${property("thermoo")}-$mc") }

		// Appleskin
		if (project.hasProperty("clothconfig")) { deps.modRuntimeOnly("me.shedaniel.cloth:cloth-config-$loader:${property("clothconfig")}") }
		deps.modImplementation("squeek.appleskin:appleskin-$loader:${property("appleskin")}")


		// Publishing
		addRequiredMod("puzzleslib")
			.modrinth("puzzles-lib") // override with Modrinth URL slug
			.addPlatform("1.21.1-neoforge", "v21.1.36-1.21.1-NeoForge") { required() }
			.addPlatform("1.21.1-fabric", "v21.1.36-1.21.1-Fabric") { required() }
			.addPlatform("1.20.1-forge", "v8.1.32-1.20.1-Forge") { required() }
			.addPlatform("1.20.1-fabric", "v8.1.32-1.20.1-Fabric") { required() }
	}
}

repositories {
	maven("https://maven.ryanliptak.com/" ) // Appleskin
	maven("https://maven.ladysnake.org/releases")
	maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
	maven("https://maven.shedaniel.me/")
	maven("https://maven.terraformersmc.com/releases/")
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