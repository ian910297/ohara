/*
 * Copyright 2019 is-land
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

import { createGlobalStyle } from 'styled-components';

export default createGlobalStyle`
  @import url('https://fonts.googleapis.com/css?family=Merriweather+Sans:400,700|Roboto:400,700,900');
  
  *, *:before, *:after {
    box-sizing: border-box;
  }


  body {
    padding: 0;
    margin: 0;
    font-family: Roboto, sans-serif;
  }

  a {
    text-decoration: none;
    
  }

  ul, li {
    margin: 0;
    padding: 0;
    list-style: none;
  }

  button, input {
    outline: none;
  }

  button {
    cursor: pointer;
  }
`;
