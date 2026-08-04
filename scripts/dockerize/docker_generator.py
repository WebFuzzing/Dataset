import json
import os
import sys

from jinja2 import Environment, FileSystemLoader
import pandas as pd


class DockerGenerator:
    def __init__(self, sut_name, expose_port):
        self.SCRIPT_LOCATION = os.path.dirname(os.path.realpath(__file__))

        self.SUT_POSTFIX = "-sut.jar"
        self.DOCKER_FILE_FOLDER = os.path.join(self.SCRIPT_LOCATION, '../../dockerfiles')

        self.sut_name = sut_name
        self.expose_port = expose_port
        self.jacoco_env_file_path = './data/.env'
        self.template_env = Environment(loader=FileSystemLoader(os.path.join(self.SCRIPT_LOCATION, "./templates")))
        self.suts = pd.read_csv(os.path.join(self.SCRIPT_LOCATION, './data/sut.csv'))
        self.sut_info = pd.read_csv(os.path.join(self.SCRIPT_LOCATION, '../../statistics/data.csv'))

        if not os.path.exists(self.DOCKER_FILE_FOLDER):
            os.makedirs(self.DOCKER_FILE_FOLDER)

        self.BASE_IMAGES = [
            {
                'name': 'JDK 8',
                'tag': 'amazoncorretto:8-alpine-jdk',
            },
            {
                'name': 'JDK 11',
                'tag': 'amazoncorretto:11-alpine-jdk',
            },
            {
                'name': 'JDK 17',
                'tag': 'amazoncorretto:17-alpine-jdk',
            },
            {
                'name': 'JDK 21',
                'tag': 'amazoncorretto:21-alpine-jdk',
            }
        ]

        if self.sut_name not in self.sut_info['NAME'].values or self.sut_name not in self.suts['NAME'].values:
            print(f"SUT {sut_name} not found in the SUT list")
            sys.exit(1)

        sut = self.suts.loc[self.suts['NAME'] == self.sut_name].iterrows().__next__()
        sut_info = self.sut_info.loc[self.sut_info['NAME'] == self.sut_name].iterrows().__next__()

        self.jdk_version = str(sut_info[1]['RUNTIME']) if str(sut_info[1]['RUNTIME']) != 'nan' else ''
        self.jvm_parameters = str(sut[1]['JVM_PARAMETERS']) if str(sut[1]['JVM_PARAMETERS']) != 'nan' else ''
        self.input_parameters = str(sut[1]['INPUT_PARAMETERS']) if str(sut[1]['INPUT_PARAMETERS']) != 'nan' else ''
        self.swagger_url = str(sut[1]['SWAGGER_URL']) if str(sut[1]['SWAGGER_URL']) != 'nan' else ''
        self.target_url = str(sut[1]['TARGET_URL']) if str(sut[1]['TARGET_URL']) != 'nan' else ''
        self.copy_additional_files = bool(sut[1]['COPY_ADDITIONAL_FILES'])
        self.database_config = json.loads(sut[1]['SERVICES']) if str(sut[1]['SERVICES']) != 'nan' else []
        self.depends_on = str(sut[1]['DEPENDS_ON']).split(';') if str(sut[1]['DEPENDS_ON']) != 'nan' else []
    # def prepare_run_docker(self):
    #     # prepare the required files
    #     shutil.copy(self.jacoco_env_file_path, self.DOCKER_FILE_FOLDER)
    #
    #     if not os.path.exists(os.path.join(self.SCRIPT_LOCATION, "dist")):
    #         os.makedirs("dist")
    #
    #     # Copy the required jar files located in EMB/dist folder. First you need to compile the SUTs.
    #     # We are copying these files because of dockerfile restrictions. We cannot copy files from parent directory.
    #     sut_filename = f"{self.sut_name}{self.SUT_POSTFIX}"
    #
    #     if not os.path.exists(f"./dist/{sut_filename}"):
    #         copy_file_name = f"../../dist/{sut_filename}"
    #         shutil.copy(copy_file_name, "./dist")
    #
    #     if not os.path.exists(f"./dist/jacocoagent.jar"):
    #         shutil.copy(f"../../dist/jacocoagent.jar", "./dist")
    #     if not os.path.exists(f"./dist/jacococli.jar"):
    #         shutil.copy(f"../../dist/jacococli.jar", "./dist")

    def get_base_image(self, jdk_version):
        base_image = None
        for image in self.BASE_IMAGES:
            if image['name'] == jdk_version:
                base_image = image['tag']
                break
        return base_image


    def generate_dockerfiles(self):
        base_image = self.get_base_image(self.jdk_version)
        save_folder_path = f"./scripts/dockerize/data/additional_files/{self.sut_name}"
        files = []
        folder_name = os.path.join(self.SCRIPT_LOCATION, f"./data/additional_files/{self.sut_name}")
        if self.copy_additional_files:
            file_list = os.listdir(folder_name)
            for file in file_list:
                files.append({
                    'source': f"{save_folder_path}/{file}",
                })

        params = {
            'BASE_IMAGE': base_image,
            'SUT_NAME': self.sut_name,
            'EMB_DIR': "./dist",
            'JVM_PARAMETERS': self.jvm_parameters,
            'INPUT_PARAMETERS': self.input_parameters,
            'ADDITIONAL_FILES': self.copy_additional_files,
            'files': files
        }

        # Create a template object from a file
        template = self.template_env.get_template("template.dockerfile")

        result = template.render(params)

        with open(os.path.join(self.SCRIPT_LOCATION, f"{self.DOCKER_FILE_FOLDER}/{self.sut_name}.dockerfile"), "w") as f:
            f.write(result)

        print(f"Created {self.sut_name}.dockerfile")


    def generate_docker_compose(self):
        params = {
            'SUT_NAME': self.sut_name,
            'EXPOSE_PORT': self.expose_port,
            'DEPENDS_ON': self.depends_on
        }

        database_template = self.template_env.get_template("db.template")
        db_images = []
        for db in self.database_config:
            health_check_command = db['health_check_command']

            database_params = {
                'name': db['name'] if 'name' in db else 'db',
                'DATABASE_IMAGE_NAME': db['image_name'],
                'DATABASE_PORT': db['port'] if 'port' in db else None,
                'TMP_FS': db['tmp_fs'],
                'DATABASE_ENVIRONMENT': str(db['environment']).split(';') if str(db['environment']) != '' else None,
                'DATABASE_VOLUME': str(db['volume']).split(';') if str(db['volume']) != '' else None,
                'HEALTH_CHECK': health_check_command != "",
                'HEALTH_CHECK_COMMAND': health_check_command,
                'COMMAND': db['command'] if 'command' in db else None,
            }
            database_image = database_template.render(database_params)
            db_images.append(database_image)
        params['DATABASES'] = db_images

        template = self.template_env.get_template("template.docker-compose.yml")
        result = template.render(params)

        with open(os.path.join(self.SCRIPT_LOCATION, f"{self.DOCKER_FILE_FOLDER}/{self.sut_name}.yaml"), "w") as f:
            non_empty_lines = [line for line in result.splitlines() if line.strip()]
            cleaned_result = '\n'.join(non_empty_lines)
            f.write(cleaned_result)
        print(f"Created {self.sut_name}.yaml")

    # def run_docker(self):
    #     # just for testing to see if the docker-compose file is working
    #     self.prepare_run_docker()
    #     docker_file = f"./dockerfiles/{self.sut_name}.yml"
    #     subprocess.run(["docker-compose", "-f", docker_file, "up", "--build", "--abort-on-container-exit", "--remove-orphans"])


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage:\ndocker_generator.py <sut_name> <expose_port>")
        sys.exit(1)

    try:
        SUT_NAME = sys.argv[1]
    except IndexError:
        print("Please provide a SUT name")
        sys.exit(1)

    try:
        EXPOSE_PORT = int(sys.argv[2])
    except IndexError:
        # default port
        EXPOSE_PORT = 8080
    #
    # try:
    #     RUN_ON_DOCKER = bool(sys.argv[3] == "True")
    # except IndexError:
    #     RUN_ON_DOCKER = False

    generator = DockerGenerator(SUT_NAME, EXPOSE_PORT)
    generator.generate_dockerfiles()
    generator.generate_docker_compose()

    # if RUN_ON_DOCKER:
    #     generator.run_docker()
