package kotlinx.html.generate

import java.io.*


fun main(args: Array<String>) {
    fillRepository()

    val packg = "kotlinx.html"
    val todir = "shared/src/main/kotlin/generated"
    val jsdir = "js/src/main/kotlin/generated"
    File(todir).mkdirs()
    File(jsdir).mkdirs()

    FileOutputStream("$todir/gen-attr-traits.kt").writer().use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.impl.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.attributeFacades.values.forEach {
                facade(it)
                emptyLine()
            }
        }
    }

    Repository.tags.values.filterIgnored().groupBy { it.name[0] }.entries.forEach { e ->
        FileOutputStream("$todir/gen-tags-${e.key}.kt").writer(Charsets.UTF_8).use {
            it.with {
                packg(packg)
                emptyLine()
                import("kotlinx.html.*")
                import("kotlinx.html.impl.*")
                import("kotlinx.html.attributes.*")
                emptyLine()

                warning()
                emptyLine()
                emptyLine()

                e.value.forEach {
                    tagClass(it, emptySet())
                }
            }
        }
    }

    FileOutputStream("$todir/gen-consumer-tags.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.impl.*")
            import("kotlinx.html.attributes.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.tags.values.filterIgnored().forEach {
                if (it.possibleChildren.isEmpty() && it.name.toLowerCase() !in emptyTags) {
                    consumerBuilderShared(it, false)
                }
                consumerBuilderShared(it, true)
                emptyLine()
            }
        }
    }

    FileOutputStream("$jsdir/gen-consumer-tags.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg + ".js")
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.impl.*")
            import("kotlinx.html.attributes.*")
            import("org.w3c.dom.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.tags.values.filterIgnored().forEach {
                if (it.possibleChildren.isEmpty() && it.name.toLowerCase() !in emptyTags && it.name.toLowerCase() !in shouldHaveNoContent) {
                    consumerBuilderJS(it, false)
                }
                consumerBuilderJS(it, true)
                emptyLine()
            }
        }
    }

    FileOutputStream("$jsdir/gen-event-attrs.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg + ".js")
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.attributes.*")
            import("kotlinx.html.dom.*")
            import("org.w3c.dom.events.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.attributeFacades.filter { it.value.attributeNames.any { it.startsWith("on") } }.forEach { facade ->
                facade.value.attributes.filter { it.name.startsWith("on") }.forEach {
                    eventProperty(facade.value.name.capitalize() + "Facade", it)
                }
            }
        }
    }

    FileOutputStream("$todir/gen-enums.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            fun genEnumAttribute(attribute: AttributeInfo) {
                if (attribute.type == AttributeType.ENUM) {
                    enum(attribute)
                } else {
                    enumObject(attribute)
                }
            }

            Repository.attributeFacades.values.forEach { facade ->
                facade.attributes.filter { it.enumValues.isNotEmpty() }.filter { !isAttributeExcluded(it.name) }.forEach { attribute ->
                    genEnumAttribute(attribute)
                }
            }

            Repository.tags.values.filterIgnored().forEach { tag ->
                tag.attributes.filter { it.enumValues.isNotEmpty() }.filter { !isAttributeExcluded(it.name) }.forEach { attribute ->
                    genEnumAttribute(attribute)
                }
            }
        }
    }

    FileOutputStream("$todir/gen-attributes.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.attributes.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.attributeDelegateRequests.toList().forEach {
                attributePseudoDelegate(it)
            }
        }
    }

    FileOutputStream("$todir/gen-tag-groups.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            import("kotlinx.html.impl.*")
            import("kotlinx.html.attributes.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            Repository.tagGroups.values.forEach { group ->
                val groupName = group.name.escapeUnsafeValues()
                clazz(Clazz(name = groupName.capitalize(), parents = listOf("Tag"), isPublic = true, isTrait = true)) {
                }
                emptyLine()
            }

            Repository.tagGroups.values.forEach { group ->
                val receiver = group.name.escapeUnsafeValues().capitalize()
                group.tags.map { Repository.tags[it] }.filterNotNull().filterIgnored().forEach {
                    htmlTagBuilders(receiver, it)
                }
            }
        }
    }

    FileOutputStream("$todir/gen-entities.kt").writer(Charsets.UTF_8).use {
        it.with {
            packg(packg)
            emptyLine()
            import("kotlinx.html.*")
            emptyLine()

            warning()
            emptyLine()
            emptyLine()

            append("enum ")
            clazz(Clazz(name = "Entities")) {
                File("generate/src/main/resources/entities.txt").readLines().filter { it.isNotEmpty() }.forEachIndexed { idx, ent ->
                    if (idx > 0) {
                        append(",")
                    }
                    indent()
                    append(ent)
                    emptyLine()
                }

                append(";")
                appendln()

                variable(Var(name = "text", type = "String"))
                appendln()
                getter()
                defineIs(StringBuilder().apply {
                    append("&".quote())
                    append(" + ")
                    receiverDot("this")
                    functionCall("toString", emptyList())
                    append(" + ")
                    append(";".quote())
                })
                appendln()
            }
        }
    }

    generateParentTraits(todir, packg)
}
