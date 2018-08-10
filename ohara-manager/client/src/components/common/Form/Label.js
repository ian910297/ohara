import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { lightBlue } from '../../../theme/variables';

const LabelWrapper = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
`;

LabelWrapper.displayName = 'Label';

const Label = ({ children, ...rest }) => {
  return <LabelWrapper {...rest}>{children}</LabelWrapper>;
};

Label.propTypes = {
  children: PropTypes.any,
};

export default Label;
