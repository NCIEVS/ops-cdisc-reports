# This script is used in the terraform external datasource. So the input is based on the standard JSON input prescribed by that resource.

# Serialize the input JSON as space separated values. For example {"key1":"value1","key2":"values2"} will turn into "key1 value1 key2 value2"
input_string=$(jq -M -r 'keys[] as $k | $k, .[$k]')

# Convert space separated values into array
input_array=(${input_string})
output="{"
# iterate through the array 2 elements at a time to capture the key and value
for ((i = 0; i < ${#input_array}; i += 2)); do
  # determine the version of the module from gradle build
  key=$(sed 's/\r$//' <<< "${input_array[$i]}")
  directory_name=$(sed 's/\r$//' <<< "${input_array[$i + 1]}")
  version=$(
    cd "$directory_name"
    ../gradlew properties -q | grep "version:" | awk '{print $2}'
  )
  # use the key that was passed and set the value as the version that was determined
  output+="\"$key\":\"$version\""
  if [[ $i+2 -lt ${#input_array} ]]; then
    output+=","
  fi
done
output+='}'

# Use jq to turn the output string into JSON
jq -n $output
