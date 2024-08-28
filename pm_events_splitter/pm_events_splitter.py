#!/usr/bin/env python3
#
# COPYRIGHT Ericsson 2022
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

import json
import os
import pathlib
import re
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile
from argparse import ArgumentParser
from pathlib import Path

EVENT_TYPE_STANDARDIZED = "STANDARDIZED"
EVENT_TYPE_NON_STANDARD = "NON_STANDARD"

PM_EVENTS_TO_OVERRIDE = ['DuPerRadioUeMeasurement', 'DuPerUeRbTrafficRep', 'DuPerUeTrafficRep']


def parse_args():
    python_script_file = Path(sys.argv[0])
    event_regulation_file = os.path.join(python_script_file.parent.parent, 'src', 'main', 'resources', 'event-mapping.yaml')

    parser = ArgumentParser()
    parser.add_argument('-i', '--input', required=True, type=str, help='Input directory path for PM Events')
    parser.add_argument('-o', '--output', required=False, type=str, default=python_script_file.parent,
                        help='Output directory path for PM Events default: $GITREPO/pm_events_splitter')
    parser.add_argument('-e', '--event_regulation_file', required=False, type=str, default=event_regulation_file,
                        help='Event regulation file default: $GITREPO/src/main/resources/event-mapping.yaml')

    args = parser.parse_args()
    return args


def camel_to_snake(s):
    return ''.join(['_' + c.lower() if c.isupper() else c for c in s]).lstrip('_')


def snake_to_camel(snake_str):
    return "".join(x.capitalize() for x in snake_str.lower().split("_"))


# removes non_standard Ericsson files from the folder
def remove_non_standard_files(protos_dir, pm_events):
    non_standard_events = pm_events[EVENT_TYPE_NON_STANDARD]

    for file in os.listdir(protos_dir):
        file_with_no_ext = os.path.splitext(file)[0]
        file_to_event = snake_to_camel(file_with_no_ext)
        if file_to_event in non_standard_events:
            os.remove(os.path.join(protos_dir, file))


# removes non_standard events from the *_function_pm_event.proto files
def remove_non_standard_content(protos_dir, pm_events):
    non_standard_events = pm_events[EVENT_TYPE_NON_STANDARD]
    function_pm_events = list(pathlib.Path(protos_dir).glob('*_function_pm_event.proto'))

    for function_pm_event in function_pm_events:
        with open(os.path.join(protos_dir, function_pm_event), "r") as file:
            lines = file.readlines()
        with open(os.path.join(protos_dir, function_pm_event), "w") as file:
            for line in lines:
                keep_line = True
                for p_event in non_standard_events:
                    p_event_snake = camel_to_snake(p_event)
                    if '/' + p_event_snake + '.proto' in line.strip("\n"):
                        keep_line = False
                    elif str(non_standard_events[p_event]) in line.strip("\n"):
                        keep_line = False
                if keep_line:
                    file.write(line)


def remove_non_standard_all(output_dir, pm_events):
    protos_dir = os.path.join(output_dir, 'pm_event')
    remove_non_standard_files(protos_dir, pm_events)
    remove_non_standard_content(protos_dir, pm_events)


def create_pmevent_zip(output_dir, zip_name):
    shutil.make_archive(zip_name, 'zip', output_dir, 'pm_event')
    print('zipfile {} created'.format(zip_name))


def load_event_id(output_dir, event_name):
    event_line = None
    event_def_file = None

    if event_name.startswith('CuCp'):
        event_def_file = os.path.join(output_dir, 'pm_event', 'cu_cp_function_pm_event.proto')
    elif event_name.startswith('Du'):
        event_def_file = os.path.join(output_dir, 'pm_event', 'du_function_pm_event.proto')
    elif event_name.startswith('CuUp'):
        event_def_file = os.path.join(output_dir, 'pm_event', 'cu_up_function_pm_event.proto')

    with open(event_def_file, 'r') as f:
        for line in f.readlines():
            if re.search(r'\b' + event_name + r'\b', line) is not None:
                event_line = line
                break

    return int(event_line.strip().split('=')[1].split(';')[0].strip())


def get_events(output_dir):
    pm_events = {
        EVENT_TYPE_STANDARDIZED: [],
        EVENT_TYPE_NON_STANDARD: []
    }
    standard_objects = []
    standard_events = {}
    proprietary_objects = []
    proprietary_events = {}

    tree = ET.parse(os.path.join(output_dir, 'pmevent.xml'))
    root = tree.getroot()
    for obj in root.findall('.//object'):
        if obj.attrib['parentDn'].endswith('STANDARDIZED'):
            standard_objects.append(obj)
        elif obj.attrib['parentDn'].endswith('PROPRIETARY'):
            proprietary_objects.append(obj)

    for obj in standard_objects:
        event_name = obj.find("./slot[@name='eventTypeId']").find('./value').text
        standard_events[event_name] = load_event_id(output_dir, event_name)

    for obj in proprietary_objects:
        event_name = obj.find("./slot[@name='eventTypeId']").find('./value').text
        proprietary_events[event_name] = load_event_id(output_dir, event_name)

    # move the PM_EVENTS_TO_OVERRIDE to standard events
    for pm_event in PM_EVENTS_TO_OVERRIDE:
        print('moving pm_event {} from non_standard to standard'.format(pm_event))
        pm_event_id = proprietary_events[pm_event]
        del proprietary_events[pm_event]
        standard_events[pm_event] = pm_event_id

    pm_events[EVENT_TYPE_STANDARDIZED] = standard_events
    pm_events[EVENT_TYPE_NON_STANDARD] = proprietary_events
    return pm_events


def prepare_json(pm_events, output_dir_path, version):
    json_data = {}
    event_mapping = {}
    event_regulation_map = {}
    for eid in pm_events[EVENT_TYPE_STANDARDIZED].values():
        event_regulation_map[eid] = EVENT_TYPE_STANDARDIZED
    for eid in pm_events[EVENT_TYPE_NON_STANDARD].values():
        event_regulation_map[eid] = EVENT_TYPE_NON_STANDARD
    event_mapping['event-regulation-map'] = event_regulation_map
    json_data['radio_node_version'] = 'radio_node_version_' + version
    json_data['event-mapping'] = event_mapping

    with open(os.path.join(output_dir_path, 'event-mapping.yaml'), 'w') as file:
        json.dump(json_data, file, indent=4)
    print('yaml file prepared', os.path.join(output_dir_path, 'event-mapping.yaml'))


def get_latest_radionode_build(input_dir):
    f_list = filter(lambda f: f.startswith("radionode-node-model-1-"), os.listdir(input_dir))
    build_n = max(f_list, key=lambda f: int(re.search(r"radionode-node-model-1-(\d*).*-jar", f).group(1)))
    return build_n


def get_latest_radionode_evenets_folder(input_dir):
    build_n = get_latest_radionode_build(input_dir)
    print('calculated latest radionode build folder -', build_n)
    latest_build_folder = os.path.join(input_dir, build_n)
    latest_events_folder = os.path.join(latest_build_folder, 'etc', 'orig-node-input', 'Events')
    print('latest_events_folder -', latest_events_folder)
    return latest_events_folder


def get_version(input_dir):
    build_n = get_latest_radionode_build(input_dir)

    version_group = re.search(r"radionode-node-model-1-(\d*)-(\d*)\.(.\d).*-jar", build_n)
    version = (version_group.group(1)
               + '_' + version_group.group(2)
               + '_' + version_group.group(3))
    print('version calculated from the radionode repo', version)
    return version


def copy_files(input_dir, output_dir):
    latest_events_folder = get_latest_radionode_evenets_folder(input_dir)
    pm_event_folder = os.path.join(output_dir, 'pm_event')

    for ne_type in ['cucp', 'cuup', 'du']:
        pm_event_proto_package = 'pm_event_proto_package_'
        ne_zip = os.path.join(latest_events_folder, pm_event_proto_package + ne_type + '.zip')
        with zipfile.ZipFile(ne_zip, 'r') as zip_ref:
            zip_ref.extractall(path=pm_event_folder)
        print('files unzipped from {} to {}'.format(ne_zip, pm_event_folder))

    # copy pmevent.xml to calculate standardized and proprietary
    latest_pm_event_xml = os.path.join(latest_events_folder, 'pmevent.xml')
    copy_pm_event_xml = os.path.join(output_dir, 'pmevent.xml')
    shutil.copy(latest_pm_event_xml, copy_pm_event_xml)
    print('file copied from {} to {}'.format(latest_pm_event_xml, copy_pm_event_xml))


def create_folder(folder):
    if not os.path.exists(folder):
        os.makedirs(folder)


def write_version(output_dir_path, version):
    output_dir = Path(output_dir_path).parent
    with open(os.path.join(output_dir, 'RADIONODE_VERSION'), 'w') as file:
        file.write(version)
    print('RADIONODE_VERSION file updated', os.path.join(output_dir, 'RADIONODE_VERSION'), version)


def clean_up(output_dir):
    shutil.rmtree(os.path.join(output_dir, 'pm_event'))
    print('removed the temp files', os.path.join(output_dir, 'pm_event'))

    os.remove(os.path.join(output_dir, 'pmevent.xml'))
    print('removed temp pm_event.xml', os.path.join(output_dir, 'pmevent.xml'))


def main():
    args = parse_args()
    input_dir_path = args.input
    output_dir_path = args.output
    event_regulation_file = args.event_regulation_file

    print('input - ', input_dir_path)
    print('output - ', output_dir_path)
    print('event_regulation_file - ', event_regulation_file)

    version = get_version(input_dir_path)
    create_folder(output_dir_path)
    copy_files(input_dir_path, output_dir_path)

    non_standard_zip = os.path.join(output_dir_path, '5G_PM_EVENT_ERICSSON_' + version)
    create_pmevent_zip(output_dir_path, non_standard_zip)

    pm_events = get_events(output_dir_path)
    prepare_json(pm_events, output_dir_path, version)

    remove_non_standard_all(output_dir_path, pm_events)
    standardized_zip = os.path.join(output_dir_path, '5G_PM_EVENT_STANDARDIZED_' + version)
    create_pmevent_zip(output_dir_path, standardized_zip)

    clean_up(output_dir_path)

    # moves/overrides the updated event-mapping in the src/main/resources folder
    shutil.move(os.path.join(output_dir_path, 'event-mapping.yaml'), event_regulation_file)
    print('event regulation file updated', event_regulation_file)

    # writes the version info in RADIONODE_VERSION file
    write_version(output_dir_path, version)


if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        print(f"An error occurred: {str(e)}")
        exit(1)
