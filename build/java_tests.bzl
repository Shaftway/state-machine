def all_java_tests(name, package=None, srcs=[], deps=[], **kwargs):
    support_files = [f for f in srcs if f[-9:] != "Test.java"]
    test_files = [f for f in srcs if f[-9:] == "Test.java"]

    package = package or '.'.join(native.package_name().split("/")[1:])

    if support_files:
        native.java_library(
            name = "_support_",
            srcs = support_files,
            deps = deps,
            **kwargs,
        )

        deps = deps + [":_support_"]

    all_tests = []
    for test_src in test_files:
        test_filename = test_src[:-5]
        test_name = name + "_" + test_filename
        all_tests.append(test_name)

        native.java_test(
            name=test_name,
            test_class=package + "." + test_filename,
            srcs=[test_src],
            deps=deps)


    native.test_suite(
        name = name,
        tests = all_tests,
    )
